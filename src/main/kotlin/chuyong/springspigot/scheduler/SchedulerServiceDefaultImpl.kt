package chuyong.springspigot.scheduler

import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitScheduler
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Service

@Service
@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
internal class SchedulerServiceDefaultImpl : SchedulerService {
    @Autowired
    private val scheduler: BukkitScheduler? = null

    @Autowired
    private val plugin: Plugin? = null
    override fun scheduleSyncDelayedTask(task: Runnable?, delay: Long): Int {
        return scheduler!!.scheduleSyncDelayedTask(plugin!!, task!!, delay)
    }

    override fun scheduleSyncDelayedTask(task: Runnable?): Int {
        return scheduler!!.scheduleSyncDelayedTask(plugin!!, task!!)
    }

    override fun scheduleSyncRepeatingTask(task: Runnable?, delay: Long, period: Long): Int {
        return scheduler!!.scheduleSyncRepeatingTask(plugin!!, task!!, delay, period)
    }

    override fun cancelTask(taskId: Int) {
        scheduler!!.cancelTask(taskId)
    }

    override fun isCurrentlyRunning(taskId: Int): Boolean {
        return scheduler!!.isCurrentlyRunning(taskId)
    }

    override fun isQueued(taskId: Int): Boolean {
        return scheduler!!.isQueued(taskId)
    }
}
