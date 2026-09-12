package com.community.assist.service;

import com.community.assist.model.*;
import com.community.assist.model.Enums.DeviceCategory;
import com.community.assist.model.Enums.ServiceEventType;
import com.community.assist.repo.FitReviewRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 尺寸复评创建（反馈处置与直接发起共用）：仅适用于护理床/轮椅类辅具。
 */
@Service
public class FitReviewService {

    private final FitReviewRepository fitReviewRepo;
    private final EventService eventService;

    public FitReviewService(FitReviewRepository fitReviewRepo, EventService eventService) {
        this.fitReviewRepo = fitReviewRepo;
        this.eventService = eventService;
    }

    public FitReview create(RentalOrder order, DeviceUnit unit, DeviceModel model,
                            Long feedbackId, String operatorName) {
        if (model.getCategory() != DeviceCategory.NURSING_BED
                && model.getCategory() != DeviceCategory.WHEELCHAIR) {
            throw BizException.badRequest("尺寸复评仅适用于护理床/轮椅类辅具，当前为「" + model.getName() + "」");
        }
        FitReview r = new FitReview();
        r.setReviewNo("FR" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + (int) (Math.random() * 900 + 100));
        r.setFeedbackId(feedbackId);
        r.setRentalOrderId(order.getId());
        r.setElderlyId(order.getElderlyId());
        r.setDeviceUnitId(unit.getId());
        fitReviewRepo.save(r);
        eventService.record(unit.getId(), order.getId(), order.getElderlyId(),
                ServiceEventType.FIT_REVIEW, "发起尺寸复评",
                "待家属上传使用照片、身高体重、房间尺寸与照护人说明", operatorName);
        return r;
    }
}
