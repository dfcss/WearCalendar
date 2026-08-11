package com.example.calendar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.material.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectAsState
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

// -------------------- 数据模型与 API 接口 --------------------
data class HolidayInfo(val name: String, val holiday: Boolean)
data class HolidayResponse(val code: Int, val data: Map<String, HolidayInfo>)

interface HolidayApi {
    @GET("api/holiday")
    suspend fun getHolidays(@Query("year") year: Int): HolidayResponse
}

// -------------------- ViewModel（真实联网获取） --------------------
class CalendarViewModel : ViewModel() {
    private val api = Retrofit.Builder()
        .baseUrl("https://timor.tech/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(HolidayApi::class.java)

    private val _holidays = MutableStateFlow<Map<String, HolidayInfo>>(emptyMap())
    val holidays: StateFlow<Map<String, HolidayInfo>> = _holidays

    init {
        viewModelScope.launch {
            try {
                val response = api.getHolidays(LocalDate.now().year)
                if (response.code == 200) {
                    _holidays.value = response.data
                }
            } catch (e: Exception) {
                // 联网失败时保持空状态，不影响UI展示
            }
        }
    }
}

// -------------------- 液态玻璃 UI + 圆形适配 --------------------
@Composable
fun CalendarScreen() {
    val vm: CalendarViewModel = viewModel()
    val holidays by vm.holidays.collectAsState()
    val now = LocalDate.now()

    // 深邃渐变背景（模拟玻璃背后的阴影空间）
    Box(
        modifier = Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0F172A), Color(0xFF1E293B))))
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 16.dp)) {

            // 顶部“液态玻璃”大日期卡片
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0x33FFFFFF)) 
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = now.dayOfMonth.toString(),
                        fontSize = 52.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "${now.month.value}月 ${now.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.CHINA)}",
                        fontSize = 16.sp,
                        color = Color.LightGray
                    )
                }
            }

            // 滚动列表（精确适配圆形表盘边缘）
            ScalingLazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp, horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items((1..now.lengthOfMonth()).toList()) { day ->
                    val date = LocalDate.of(now.year, now.month, day)
                    val holidayInfo = holidays[date.toString()]
                    val isToday = date == now

                    // 每日毛玻璃卡片
                    Card(
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isToday) Color(0xFF3B82F6).copy(alpha = 0.8f)
                                         else Color(0x22FFFFFF)
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // 日期数字
                            Text(
                                text = day.toString().padStart(2, '0'),
                                fontSize = 20.sp,
                                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                color = if (isToday) Color.White else Color(0xFFE2E8F0)
                            )

                            // 节假日与调休状态
                            if (holidayInfo != null) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(
                                                if (holidayInfo.holiday) Color(0xFF10B981) else Color(0xFFEF4444),
                                                RoundedCornerShape(50%)
                                            )
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = holidayInfo.name,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (holidayInfo.holiday) Color(0xFF34D399) else Color(0xFFF87171)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------- 入口 Activity --------------------
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { CalendarScreen() }
    }
}
