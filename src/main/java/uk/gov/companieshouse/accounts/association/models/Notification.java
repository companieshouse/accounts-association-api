package uk.gov.companieshouse.accounts.association.models;

import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

public class Notification {


    @Field("notified_at")
    public LocalDateTime notifiedAt;

    @Field("notification_event")
    public String notificationEvent;

    public Notification() {
    }

    public LocalDateTime getNotifiedAt() {
        return notifiedAt;
    }

    public void setNotifiedAt(LocalDateTime notifiedAt) {
        this.notifiedAt = notifiedAt;
    }

    public String getNotificationEvent() {
        return notificationEvent;
    }

    public void setNotificationEvent(String notificationEvent) {
        this.notificationEvent = notificationEvent;
    }


//    @Override
//    public String toString() {
//        return "Notification{" +
//                "notifiedAt=" + notifiedAt +
//                ", notificationEvent='" + notificationEvent + '\'' +
//
//                '}';
//    }
}
