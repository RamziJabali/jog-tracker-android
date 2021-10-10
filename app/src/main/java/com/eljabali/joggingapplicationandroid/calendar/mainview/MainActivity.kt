package com.eljabali.joggingapplicationandroid.calendar.mainview

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import com.eljabali.joggingapplicationandroid.R
import com.eljabali.joggingapplicationandroid.statistics.view.JogStatisticsFragment
import com.eljabali.joggingapplicationandroid.calendar.mainviewmodel.ViewModel
import com.eljabali.joggingapplicationandroid.calendar.recyclerview.RecyclerViewFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.roomorama.caldroid.CaldroidFragment
import com.roomorama.caldroid.CaldroidListener
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers
import org.koin.androidx.viewmodel.ext.android.viewModel
import zoneddatetime.ZonedDateTimes
import java.util.Date

class MainActivity : AppCompatActivity(), ViewListener {

    companion object {
        const val CAL_TAG = "CaldroidFragment"
        const val RCV_TAG = "RecyclerViewFragment"
        const val MVM_TAG = "MainViewModel"
    }

    private var viewState: ViewState = ViewState()

    private val viewModel: ViewModel by viewModel()

    private val compositeDisposable: CompositeDisposable = CompositeDisposable()
    private val bottomNavigationBarView: BottomNavigationView by lazy { findViewById(R.id.bottom_navigation) }

    private val statisticsFragment: JogStatisticsFragment by lazy { JogStatisticsFragment.newInstance() }
    private val recyclerViewFragment: RecyclerViewFragment by lazy { RecyclerViewFragment.newInstance() }
    private val caldroidFragment: CaldroidFragment by lazy {
        val today = ZonedDateTimes.today
        CaldroidFragment().apply {
            arguments = Bundle().apply {
                putInt(CaldroidFragment.MONTH, today.monthValue)
                putInt(CaldroidFragment.YEAR, today.year)
                putBoolean(CaldroidFragment.ENABLE_CLICK_ON_DISABLED_DATES, false)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupBottomNavigation()
        setupFragments()
        setupCalendarListener()
        monitorCalendarViewState()
    }

    override fun monitorCalendarViewState() {
        viewModel.viewStateObservable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { viewState ->
                        this.viewState = viewState
                        setNewViewState(viewState)
                    },
                    { error -> Log.e(MVM_TAG, error.localizedMessage, error) })
            .addTo(compositeDisposable)
    }

    override fun setNewViewState(viewState: ViewState) {
        viewState.listOfColoredDates.forEach { date ->
            caldroidFragment.setBackgroundDrawableForDate(date.colorDrawable, date.date)
        }
        recyclerViewFragment.updateListOfProperties(viewState.listOfSpecificDates)
        caldroidFragment.refreshView()
    }


    private fun setupFragments() {
        supportFragmentManager.beginTransaction()
            .add(R.id.frameLayout, caldroidFragment, CAL_TAG)
            .hide(caldroidFragment)
            .commit()
        supportFragmentManager.beginTransaction()
            .add(R.id.recycler_view_frame_layout, recyclerViewFragment, RCV_TAG)
            .hide(recyclerViewFragment)
            .commit()
        supportFragmentManager.beginTransaction()
            .add(R.id.frameLayout, statisticsFragment, JogStatisticsFragment.TAG)
            .commit()
    }

    private fun setupBottomNavigation() {
        bottomNavigationBarView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.statistics_page -> {
                    supportFragmentManager.beginTransaction()
                        .hide(caldroidFragment)
                        .hide(recyclerViewFragment)
                        .show(statisticsFragment)
                        .commit()
                    true
                }
                R.id.calendar_page -> {
                    viewModel.getAllEntries()
                    supportFragmentManager.beginTransaction()
                        .hide(statisticsFragment)
                        .show(caldroidFragment)
                        .show(recyclerViewFragment)
                        .commit()
                    true
                }
                else -> false
            }
        }

        bottomNavigationBarView.setOnItemReselectedListener { item ->
            when (item.itemId) {
                R.id.statistics_page -> {
                    Toast.makeText(this, "StatsPage reselected", Toast.LENGTH_SHORT).show()
                }
                R.id.calendar_page -> {
                    Toast.makeText(this, "CalendarPage reselected", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupCalendarListener() {
        caldroidFragment.caldroidListener = object : CaldroidListener() {
            override fun onSelectDate(date: Date, view: View?) {
                viewModel.getAllJogsAtSpecificDate(date)
            }
        }
    }
}