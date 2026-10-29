package com.huanchengfly.tieba.post

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** Compose 侧的偏好读写：只读收集不会写盘，用户改动会立即在进程级作用域落盘。 */
/**
 * 可读写偏好。语义：
 * 1. 只读状态下不会写盘——首帧的 [defaultValue] 绝不覆盖已存值；
 * 2. 用户改动**立即**写盘，且写盘跑在进程级作用域里，页面随即被销毁/切走也不会丢；
 * 3. DataStore 里的真实值到达时同步进状态（外部改动也能反映到开关上）。
 *
 * 早期实现把写盘放在 `LaunchedEffect(state.value)` 中：一是首帧就写默认值，覆盖用户设置；
 * 二是协程随页面离开而被取消，改动可能没落盘——两者都会表现为「进子页返回后开关被重置」。
 */
@Composable
fun <T> rememberPreferenceAsMutableState(
    key: Preferences.Key<T>,
    defaultValue: T
): MutableState<T> {
    val dataStore = LocalContext.current.dataStore
    // 只有「外部同步」和「用户输入」两个来源会改变它
    val state = remember { mutableStateOf(defaultValue) }
    val applier = remember(dataStore, key) {
        PreferenceApplier<T>(dataStore)
    }

    LaunchedEffect(key) {
        dataStore.data.map { it[key] ?: defaultValue }.distinctUntilChanged()
            .collect {
                // 用户刚写下的值可能先一步到达这里，避免把界面抖回旧值
                if (!applier.hasPendingWrite || it != state.value) {
                    state.value = it
                }
            }
    }

    // 写盘用进程级作用域：调用方一改就发出去，页面销毁不会取消它
    return remember(state, applier, key) {
        PreferenceState(
            delegate = state,
            onWrite = { value -> ProcessScope.launch { applier.write(key, value) } },
        )
    }
}

/**
 * 包一层 [MutableState]：读取直通，写入先改界面再异步落盘。
 * 用独立实现而不是匿名对象，避免和 [MutableState] 的解构声明冲突。
 */
private class PreferenceState<T>(
    private val delegate: MutableState<T>,
    private val onWrite: (T) -> Unit,
) : MutableState<T> {
    override var value: T
        get() = delegate.value
        set(value) {
            if (delegate.value == value) return
            delegate.value = value
            onWrite(value)
        }

    override fun component1(): T = value

    override fun component2(): (T) -> Unit = { value = it }
}

/** 进程级协程作用域：写盘不随页面生命周期取消。 */
private val ProcessScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

/**
 * 记录某个偏好是否有尚未落盘的写入，避免 DataStore 回发的旧值把界面抖回去。
 */
private class PreferenceApplier<T>(private val dataStore: DataStore<Preferences>) {
    @Volatile
    var hasPendingWrite: Boolean = false
        private set

    suspend fun write(key: Preferences.Key<T>, value: T) {
        hasPendingWrite = true
        try {
            dataStore.edit { it[key] = value }
        } finally {
            hasPendingWrite = false
        }
    }
}

/**
 * 只读偏好。原实现会顺手把值写回 DataStore，同样存在「默认值覆盖已存值」的问题；
 * 只读场景不应产生任何写入。
 */
@Composable
fun <T> rememberPreferenceAsState(
    key: Preferences.Key<T>,
    defaultValue: T
): State<T> {
    val dataStore = LocalContext.current.dataStore
    val state = remember { mutableStateOf(defaultValue) }

    LaunchedEffect(key) {
        dataStore.data.map { it[key] ?: defaultValue }.distinctUntilChanged()
            .collect { state.value = it }
    }

    return state
}

@SuppressLint("FlowOperatorInvokedInComposition")
@Composable
fun <T> DataStore<Preferences>.collectPreferenceAsState(
    key: Preferences.Key<T>,
    defaultValue: T
): State<T> {
    return data.map { it[key] ?: defaultValue }.collectAsState(initial = defaultValue)
}
