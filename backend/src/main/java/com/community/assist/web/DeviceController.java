package com.community.assist.web;

import com.community.assist.model.*;
import com.community.assist.model.Enums.DeviceStatus;
import com.community.assist.model.Enums.Role;
import com.community.assist.model.Enums.ServiceEventType;
import com.community.assist.model.Enums.SubsidyStatus;
import com.community.assist.repo.*;
import com.community.assist.service.BizException;
import com.community.assist.service.CurrentUser;
import com.community.assist.service.EventService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 辅具库存 + 回收消毒质检 + 单件辅具生命周期档案。
 */
@RestController
@RequestMapping("/api/devices")
public class DeviceController {

    private final DeviceModelRepository modelRepo;
    private final DeviceUnitRepository unitRepo;
    private final ServiceEventRepository eventRepo;
    private final RentalOrderRepository rentalRepo;
    private final ElderlyRepository elderlyRepo;
    private final SubsidyRepository subsidyRepo;
    private final EventService eventService;
    private final CurrentUser currentUser;

    public DeviceController(DeviceModelRepository modelRepo, DeviceUnitRepository unitRepo,
                            ServiceEventRepository eventRepo, RentalOrderRepository rentalRepo,
                            ElderlyRepository elderlyRepo, SubsidyRepository subsidyRepo,
                            EventService eventService, CurrentUser currentUser) {
        this.modelRepo = modelRepo;
        this.unitRepo = unitRepo;
        this.eventRepo = eventRepo;
        this.rentalRepo = rentalRepo;
        this.elderlyRepo = elderlyRepo;
        this.subsidyRepo = subsidyRepo;
        this.eventService = eventService;
        this.currentUser = currentUser;
    }

    @GetMapping("/models")
    public List<Map<String, Object>> models() {
        currentUser.get();
        List<Map<String, Object>> res = new ArrayList<>();
        for (DeviceModel m : modelRepo.findAll()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("model", m);
            row.put("total", unitRepo.countByModelId(m.getId()));
            row.put("available", unitRepo.countByModelIdAndStatus(m.getId(), DeviceStatus.IN_STOCK));
            row.put("leased", unitRepo.countByModelIdAndStatus(m.getId(), DeviceStatus.LEASED));
            res.add(row);
        }
        return res;
    }

    @PostMapping("/models")
    public DeviceModel createModel(@RequestBody DeviceModel req) {
        currentUser.requireAny(Role.ADMIN);
        req.setId(null);
        return modelRepo.save(req);
    }

    @GetMapping("/units")
    public List<Map<String, Object>> units(@RequestParam(required = false) DeviceStatus status,
                                           @RequestParam(required = false) Long modelId) {
        currentUser.get();
        List<DeviceUnit> src;
        if (status != null) {
            src = unitRepo.findByStatusOrderByIdDesc(status);
        } else if (modelId != null) {
            src = unitRepo.findByModelIdOrderByIdDesc(modelId);
        } else {
            src = unitRepo.findAllByOrderByIdDesc();
        }
        List<Map<String, Object>> res = new ArrayList<>();
        for (DeviceUnit u : src) {
            res.add(unitView(u));
        }
        return res;
    }

    public record UnitReq(Long modelId, String serialNo, String conditionNote) {
    }

    @PostMapping("/units")
    public Map<String, Object> createUnit(@RequestBody UnitReq req) {
        currentUser.requireAny(Role.ADMIN, Role.WAREHOUSE);
        DeviceModel m = modelRepo.findById(req.modelId()).orElseThrow(() -> BizException.notFound("型号不存在"));
        if (req.serialNo() == null || req.serialNo().isBlank()) {
            throw BizException.badRequest("序列号不能为空");
        }
        DeviceUnit u = new DeviceUnit();
        u.setModelId(m.getId());
        u.setSerialNo(req.serialNo().trim());
        u.setConditionNote(req.conditionNote());
        u.setPurchaseDate(java.time.LocalDate.now());
        return unitView(unitRepo.save(u));
    }

    /**
     * 辅具维度档案：一件辅具服务过哪些家庭、经历了哪些服务动作。
     */
    @GetMapping("/units/{id}/lifecycle")
    public Map<String, Object> lifecycle(@PathVariable Long id) {
        currentUser.get();
        DeviceUnit u = unitRepo.findById(id).orElseThrow(() -> BizException.notFound("辅具不存在"));
        Map<String, Object> m = unitView(u);
        m.put("events", eventRepo.findByDeviceUnitIdOrderByIdDesc(id));

        List<Map<String, Object>> history = new ArrayList<>();
        for (RentalOrder o : rentalRepo.findByDeviceUnitIdOrderByIdDesc(id)) {
            Map<String, Object> h = new LinkedHashMap<>();
            h.put("orderNo", o.getOrderNo());
            h.put("status", o.getStatus().name());
            h.put("startDate", o.getStartDate());
            h.put("endDate", o.getEndDate());
            elderlyRepo.findById(o.getElderlyId()).ifPresent(e -> {
                h.put("elderlyName", e.getName());
                h.put("caregiverName", e.getCaregiverName());
                h.put("community", e.getCommunity());
            });
            history.add(h);
        }
        m.put("rentalHistory", history);
        return m;
    }

    /** 回收入库后开始消毒质检 */
    @PostMapping("/units/{id}/disinfect")
    @Transactional
    public Map<String, Object> disinfect(@PathVariable Long id) {
        User op = currentUser.requireAny(Role.WAREHOUSE, Role.ADMIN);
        DeviceUnit u = unitRepo.findById(id).orElseThrow(() -> BizException.notFound("辅具不存在"));
        if (u.getStatus() != DeviceStatus.RECALLED) {
            throw BizException.badRequest("仅「已回收待消毒」的辅具可开始消毒");
        }
        u.setStatus(DeviceStatus.DISINFECTING);
        unitRepo.save(u);
        eventService.record(u.getId(), null, null, ServiceEventType.DISINFECT, "开始消毒质检",
                "辅具 " + u.getSerialNo() + " 进入消毒质检流程", op.getName());
        return unitView(u);
    }

    public record QcReq(Boolean pass, String note) {
    }

    /** 消毒质检结果：合格→再上架（影响库存周转与补贴核销）；不合格→报废 */
    @PostMapping("/units/{id}/qc")
    @Transactional
    public Map<String, Object> qc(@PathVariable Long id, @RequestBody QcReq req) {
        User op = currentUser.requireAny(Role.WAREHOUSE, Role.ADMIN);
        DeviceUnit u = unitRepo.findById(id).orElseThrow(() -> BizException.notFound("辅具不存在"));
        if (u.getStatus() != DeviceStatus.DISINFECTING) {
            throw BizException.badRequest("仅「消毒质检中」的辅具可录入质检结果");
        }
        boolean pass = req.pass() != null && req.pass();
        String note = req.note() == null ? "" : req.note();
        if (pass) {
            u.setStatus(DeviceStatus.IN_STOCK);
            u.setRentalCount(u.getRentalCount() + 1);
            u.setConditionNote("质检合格：" + note);
            eventService.record(u.getId(), null, null, ServiceEventType.QC, "消毒质检合格", note, op.getName());
            eventService.record(u.getId(), null, null, ServiceEventType.RESTOCK, "再次上架",
                    "辅具 " + u.getSerialNo() + " 重新进入可租库存", op.getName());
        } else {
            u.setStatus(DeviceStatus.SCRAPPED);
            u.setConditionNote("质检不合格报废：" + note);
            eventService.record(u.getId(), null, null, ServiceEventType.QC, "消毒质检不合格", note, op.getName());
            eventService.record(u.getId(), null, null, ServiceEventType.SCRAP, "报废出库",
                    "辅具 " + u.getSerialNo() + " 不再投入租赁", op.getName());
        }
        unitRepo.save(u);
        writeOffSubsidies(u, op);
        return unitView(u);
    }

    /** 租赁结案且辅具完成回收质检后，核销该租赁已通过的补贴 */
    private void writeOffSubsidies(DeviceUnit u, User op) {
        rentalRepo.findFirstByDeviceUnitIdOrderByIdDesc(u.getId()).ifPresent(order -> {
            for (SubsidyApplication s : subsidyRepo.findByRentalOrderIdOrderByIdDesc(order.getId())) {
                if (s.getStatus() == SubsidyStatus.APPROVED) {
                    s.setStatus(SubsidyStatus.WRITTEN_OFF);
                    s.setWrittenOffAt(LocalDateTime.now());
                    subsidyRepo.save(s);
                    eventService.record(u.getId(), order.getId(), order.getElderlyId(),
                            ServiceEventType.CLOSE, "补贴核销",
                            "订单 " + order.getOrderNo() + " 补贴 ¥" + s.getAmount() + " 已核销", op.getName());
                }
            }
        });
    }

    private Map<String, Object> unitView(DeviceUnit u) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("unit", u);
        modelRepo.findById(u.getModelId()).ifPresent(dm -> {
            m.put("modelName", dm.getName());
            m.put("category", dm.getCategory().name());
        });
        return m;
    }
}
