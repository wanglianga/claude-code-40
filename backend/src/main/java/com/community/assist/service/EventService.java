package com.community.assist.service;

import com.community.assist.model.Enums.ServiceEventType;
import com.community.assist.model.ServiceEvent;
import com.community.assist.repo.ServiceEventRepository;
import org.springframework.stereotype.Service;

/**
 * 辅具生命周期事件记录：所有租赁/维修/回收/消毒/上架动作都落到同一条事件流。
 */
@Service
public class EventService {

    private final ServiceEventRepository eventRepository;

    public EventService(ServiceEventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    public ServiceEvent record(Long deviceUnitId, Long rentalOrderId, Long elderlyId,
                               ServiceEventType type, String title, String detail, String operatorName) {
        ServiceEvent e = new ServiceEvent();
        e.setDeviceUnitId(deviceUnitId);
        e.setRentalOrderId(rentalOrderId);
        e.setElderlyId(elderlyId);
        e.setType(type);
        e.setTitle(title);
        e.setDetail(detail);
        e.setOperatorName(operatorName);
        return eventRepository.save(e);
    }
}
