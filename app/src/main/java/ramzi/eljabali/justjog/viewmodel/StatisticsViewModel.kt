package ramzi.eljabali.justjog.viewmodel

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ramzi.eljabali.justjog.util.getFormattedTimeSeconds
import com.jaikeerthick.composable_graphs.composables.line.model.LineData
import javatimefun.zoneddatetime.ZonedDateTimes
import javatimefun.zoneddatetime.extensions.compareDay
import javatimefun.zoneddatetime.extensions.getLast
import javatimefun.zoneddatetime.extensions.getNext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DayOfWeek
import ramzi.eljabali.justjog.loactionservice.ForegroundService
import ramzi.eljabali.justjog.motivationalquotes.MotivationQuotesAPI
import ramzi.eljabali.justjog.usecase.JogUseCase
import ramzi.eljabali.justjog.usecase.ModifiedJogSummary
import ramzi.eljabali.justjog.util.DurationFormat
import ramzi.eljabali.justjog.util.TAG
import ramzi.eljabali.justjog.viewstate.StatisticsViewState
import java.time.Duration
import java.time.ZonedDateTime
import java.util.Locale
import kotlin.math.ceil


class StatisticsViewModel(
    private val jogUseCase: JogUseCase,
    private val motivationQuotesAPI: MotivationQuotesAPI,
    private val applicationContext: Context
) : ViewModel() {

    private val visiblePermissionDialogQueue = mutableStateListOf<String>()
    private val _statisticsViewState = MutableStateFlow(StatisticsViewState())
    val statisticsViewState: StateFlow<StatisticsViewState> = _statisticsViewState.asStateFlow()

    companion object {
        private val daysOfTheWeek = listOf("Mon", "Tues", "Wed", "Thurs", "Fri", "Sat", "Sun")
    }

    fun onLaunch() {
        viewModelScope.launch {
            val quote = getQuote()
            val startOfWeekDate = ZonedDateTimes.today.getLast(
                DayOfWeek.MONDAY,
                countingInThisDay = true
            )
            val endOfWeekDate =
                ZonedDateTimes.today.getNext(DayOfWeek.SUNDAY, countingInThisDay = true)
            val thisWeeksJogSummaries = getWeeklyJogSummaries(startOfWeekDate, endOfWeekDate)
            val chartStatistics =
                getChartData(thisWeeksJogSummaries, startOfWeekDate)
            val weeklyStatisticsBreakDown =
                getWeeklyStatisticsBreakDown(thisWeeksJogSummaries)
            val jogStatisticsBreakDown = getWeeklyPerJogStatisticsBreakDown(thisWeeksJogSummaries)
            _statisticsViewState.update {
                it.copy(
                    quote = quote,
                    lineDataList = chartStatistics,
                    weeklyStatisticsBreakDown = weeklyStatisticsBreakDown,
                    perJogStatisticsBreakDown = jogStatisticsBreakDown
                )
            }
        }
    }

    private fun getChartData(
        summaryList: List<ModifiedJogSummary>,
        startOfWeek: ZonedDateTime,
    ): List<LineData> {
        if (summaryList.isEmpty()) {
            return listOf(
                LineData(x = "Mon", y = 0),
                LineData(x = "Tues", y = 0),
                LineData(x = "Wed", y = 0),
                LineData(x = "Thurs", y = 0),
                LineData(x = "Fri", y = 0),
                LineData(x = "Sat", y = 0),
                LineData(x = "Sun", y = 0),
            )
        }
        val listOfLineData = mutableListOf<LineData>()
        var tempDate = startOfWeek
        var index = 0
        for (day in 0..6) {
            if (index >= summaryList.size || tempDate.compareDay(summaryList[index].startDate) != 0) {
                listOfLineData.add(LineData(x = daysOfTheWeek[day], y = 0))
            } else if (index < summaryList.size) {
                if (tempDate.compareDay(summaryList[index].startDate) == 0) {
                    var totalDistance = 0.0
                    while (tempDate.compareDay(summaryList[index].startDate) == 0) {
                        totalDistance += summaryList[index].totalDistance
                        index++
                        if (index >= summaryList.size) {
                            break
                        }
                    }
                    listOfLineData.add(
                        LineData(
                            x = daysOfTheWeek[day],
                            y = ceil(totalDistance)
                        )
                    )
                }
            }
            tempDate = tempDate.plusDays(1)
        }
        Log.i(TAG, "getChartData: $listOfLineData")
        return listOfLineData
    }

    private fun getWeeklyStatisticsBreakDown(
        weeklyJogSummaries: List<ModifiedJogSummary>
    ): List<String> {
        if (weeklyJogSummaries.isEmpty()) {
            return listOf(
                "0 Miles",
                "No Weekly Data",
                "No Weekly Data"
            )
        }
        val totalJogs = "${weeklyJogSummaries.size} Runs"
        var totalMiles = 0.0
        var totalDuration = Duration.ZERO
        for (summary in weeklyJogSummaries) {
            totalMiles += summary.totalDistance
            totalDuration = totalDuration.plus(summary.duration)
        }
        return listOf(
            totalJogs,
            String.format(Locale.US, "%.2f Miles", totalMiles),
            getFormattedTimeSeconds(totalDuration.seconds, DurationFormat.HMS)
        )
    }

    private fun getWeeklyPerJogStatisticsBreakDown(weeklyJogSummaries: List<ModifiedJogSummary>): List<String> {
        if (weeklyJogSummaries.isEmpty()) {
            return listOf(
                "0 Miles",
                "No Weekly Data",
                "No Weekly Data"
            )
        }
        val averageMilesPerJog =
            weeklyJogSummaries.sumOf { it.totalDistance } / weeklyJogSummaries.size
        val averageDurationPerJog =
            weeklyJogSummaries.sumOf { it.duration.seconds } / weeklyJogSummaries.size
        val minutesPerMile = (averageDurationPerJog / 60.0) / averageMilesPerJog
        return listOf(
            String.format(Locale.US, "%.2f Miles", averageMilesPerJog),
            String.format(Locale.US, "%.2f Mins/Mil", minutesPerMile),
            getFormattedTimeSeconds(averageDurationPerJog, DurationFormat.MS)
        )
    }

    private suspend fun getWeeklyJogSummaries(
        startOfWeek: ZonedDateTime,
        endOfWeek: ZonedDateTime
    ): List<ModifiedJogSummary> {
        val result = viewModelScope.async {
            jogUseCase.getJogSummariesBetweenDates(
                startOfWeek, endOfWeek
            ).first()
        }.await()
        return result
    }


    private suspend fun getQuote(): String {
        val quote = viewModelScope.async(Dispatchers.IO) {
            var body = ""
            try {
                val result = motivationQuotesAPI.getQuote().body()
                Log.i(this.TAG, "getQuote: $result")
                body = if (!result.isNullOrEmpty()) {
                    Log.i(this.TAG, "getQuote: Successful")
                    result[0].quote
                } else {
                    Log.i(this.TAG, "getQuote: Failed")
                    "Seize the day, not the person."
                }
            } catch (e: Exception) {
                Log.e(this.TAG, "${e.message}")
            }
            body
        }
        return quote.await()
    }

    /*

        PERMISSION HANDLING, SHOWING, AND DISMISSING

     */

    fun dismissDialog() {
        visiblePermissionDialogQueue.removeFirst()
        _statisticsViewState.update {
            it.copy(
                shouldBlur = false
            )
        }
    }

    fun onFabClicked() {
        Log.i(this.TAG, "onFabClicked: start")
        if (!visiblePermissionDialogQueue.isEmpty()) {
            _statisticsViewState.update {
                it.copy(
                    shouldBlur = true,
                    listOfPermissions = visiblePermissionDialogQueue
                )
            }
            return
        }
        Intent(applicationContext, ForegroundService::class.java).also {
            it.action = ForegroundService.Actions.START.name
            Log.i(this.TAG, "onFabClicked: ${it.action}")
            applicationContext.startService(it)
        }
    }

    fun onPermissionResult(
        permission: String,
        isGranted: Boolean
    ) {
        if (!isGranted && !visiblePermissionDialogQueue.contains(permission)) {
            visiblePermissionDialogQueue.add(permission)
        }
    }
}