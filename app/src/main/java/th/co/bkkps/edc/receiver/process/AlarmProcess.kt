package th.co.bkkps.edc.receiver.process

interface AlarmProcess {
    fun enableBroadcastReceiver(isEnable: Boolean, clazz: Class<*>)
    fun registerAlarmManager()
    fun detectAndRunPendingItems()
}