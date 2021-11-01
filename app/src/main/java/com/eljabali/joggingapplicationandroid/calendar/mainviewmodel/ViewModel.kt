package com.eljabali.joggingapplicationandroid.calendar.mainviewmodel

import android.app.Application
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import com.eljabali.joggingapplicationandroid.util.DateFormat
import com.eljabali.joggingapplicationandroid.util.getFormattedTime
import com.eljabali.joggingapplicationandroid.util.getMPH
import com.eljabali.joggingapplicationandroid.util.getTotalDistance
import com.eljabali.joggingapplicationandroid.calendar.mainview.ColoredDates
import com.eljabali.joggingapplicationandroid.calendar.mainview.ViewState
import com.eljabali.joggingapplicationandroid.calendar.recyclerview.RecyclerViewProperties
import com.eljabali.joggingapplicationandroid.data.usecase.ModifiedJogDateInformation
import com.eljabali.joggingapplicationandroid.data.usecase.ModifiedJogSummary
import com.eljabali.joggingapplicationandroid.data.usecase.UseCase
import com.google.android.gms.maps.model.LatLng
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import zoneddatetime.extensions.print
import java.time.Duration
import java.time.ZonedDateTime
import java.util.*

class ViewModel(application: Application, private val useCase: UseCase) :
    AndroidViewModel(application) {

    companion object {
        const val VM_TAG = "ViewModel"
        val hasWorkedOutColor: ColorDrawable =
            ColorDrawable(Color.GREEN)
        val hasNotWorkedOutColor: ColorDrawable =
            ColorDrawable(Color.RED)
        val noJogRecorded: ColorDrawable =
            ColorDrawable(Color.WHITE)
    }

    private val compositeDisposable = CompositeDisposable()
    private var viewState = ViewState()

    val viewStateObservable = BehaviorSubject.create<ViewState>()

    fun getAllDates() {
        useCase.getAllJogSummaries()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { allJogSummaries ->
                    viewState = viewState.copy(
                        listOfColoredDates = getColoredDatesFromJogSummary(listOfJogSummary = allJogSummaries)
                    )
                    invalidateView()
                },
                { error -> Log.e(VM_TAG, error.localizedMessage, error) }
            ).addTo(compositeDisposable)
    }


    fun getAllJogsAtSpecificDate(date: Date) {
        useCase.getAllJogsAtSpecificDate(date)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { listOfSpecificDates ->
                    viewState = viewState.copy(
                        listOfSpecificDates = convertModifiedJoggingListToRecyclerViewProperties(
                            listOfSpecificDates
                        )
                    )
                    invalidateView()
                },
                { error -> Log.e(VM_TAG, error.localizedMessage, error) }
            )
            .addTo(compositeDisposable)
    }


    private fun convertModifiedJoggingListToRecyclerViewProperties(listOfModifiedJogDateInformation: List<ModifiedJogDateInformation>): List<RecyclerViewProperties> {
        val listOfRecyclerViewProperties = mutableListOf<RecyclerViewProperties>()
        val listOfLatLng = mutableListOf<LatLng>()
        val listOfTime = mutableListOf<ZonedDateTime>()
        var index = 0
        var currentEntry = 0
        if (listOfModifiedJogDateInformation.isNotEmpty()) {
            currentEntry = listOfModifiedJogDateInformation[0].runNumber
        }
        var currentRun = 1
        listOfModifiedJogDateInformation.forEach { modifiedJogDateInformation ->
            if (modifiedJogDateInformation.runNumber == currentEntry) {
                listOfLatLng.add(modifiedJogDateInformation.latitudeLongitude)
                listOfTime.add(modifiedJogDateInformation.dateTime)
            }
            if (currentEntry != modifiedJogDateInformation.runNumber || index == listOfModifiedJogDateInformation.size - 1) {
                listOfRecyclerViewProperties.add(
                    RecyclerViewProperties(
                        totalDistance = getTotalDistance(listOfLatLng).toString(),
                        totalTime = getFormattedTime(
                            Duration.between(
                                listOfTime[0],
                                listOfTime[listOfTime.size - 1]
                            ).seconds
                        ),
                        jogEntry = currentRun.toString(),
                        milesPerHour = getMPH(
                            getTotalDistance(listOfLatLng), Duration.between(
                                listOfTime[0],
                                listOfTime[listOfTime.size - 1]
                            ).seconds
                        ),
                        date = modifiedJogDateInformation.dateTime.print(DateFormat.YYYY_MM_DD.format)
                    )
                )
                listOfLatLng.clear()
                listOfTime.clear()
                currentEntry = modifiedJogDateInformation.runNumber
                currentRun++
            }
            index++
        }
        return listOfRecyclerViewProperties
    }

    private fun getColoredDatesFromJogSummary(listOfJogSummary: List<ModifiedJogSummary>): List<ColoredDates> {
        val listOfColoredDates: MutableList<ColoredDates> = mutableListOf()
        if (listOfJogSummary.isEmpty()) {
            viewState.listOfColoredDates.forEach { coloredDates ->
                listOfColoredDates.add(ColoredDates(coloredDates.date, noJogRecorded))
            }
            return listOfColoredDates
        }
        var date = listOfJogSummary[0].date
        var index = 0
        while (date < ZonedDateTime.now()) {
            if (index < listOfJogSummary.size && listOfJogSummary[index].date == date) {
                listOfColoredDates.add(
                    ColoredDates(
                        date = convertZonedDateTimeToDate(date),
                        hasWorkedOutColor
                    )
                )
                index++
            } else {
                listOfColoredDates.add(
                    ColoredDates(
                        date = convertZonedDateTimeToDate(date),
                        hasNotWorkedOutColor
                    )
                )
            }
            date = date.plusDays(1)
        }
        return listOfColoredDates
    }

    private fun convertZonedDateTimeToDate(dateTime: ZonedDateTime): Date =
        Date.from(dateTime.toInstant())

    private fun invalidateView() {
        viewStateObservable.onNext(viewState)
    }
}