package com.udacity.catpoint.data;

import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * Interface showing the methods our security repository will need to support
 */
public interface SecurityRepository extends Cloneable {
    void addSensor(@NotNull Sensor sensor);
    void removeSensor(@NotNull Sensor sensor);
    void updateSensor(@NotNull Sensor sensor);
    void setAlarmStatus(@NotNull AlarmStatus alarmStatus);
    void setArmingStatus(@NotNull ArmingStatus armingStatus);
    Set<Sensor> getSensors();
    AlarmStatus getAlarmStatus();
    ArmingStatus getArmingStatus();
}
