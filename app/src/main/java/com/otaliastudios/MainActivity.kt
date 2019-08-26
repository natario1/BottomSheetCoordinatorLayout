package com.otaliastudios

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
import com.otaliastudios.bottomsheetcoordinatorlayout.BottomSheetCoordinatorBehavior
import kotlinx.android.synthetic.main.bottom_sheet_peek_search.*
import kotlinx.android.synthetic.main.bottom_sheet_search.*
import net.kibotu.android.recyclerviewpresenter.PresenterAdapter
import net.kibotu.android.recyclerviewpresenter.PresenterModel
import net.kibotu.logger.LogcatLogger
import net.kibotu.logger.Logger
import net.kibotu.logger.Logger.logv


class MainActivity : AppCompatActivity() {

    val filterAdapter = PresenterAdapter()

    val resultAdapter = PresenterAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Logger.addLogger(LogcatLogger())

        logv { "requestApplyInsets init" }

        search_bottom_sheet.setHideable(false)
        search_bottom_sheet.state = BottomSheetCoordinatorBehavior.STATE_HALF_EXPANDED
        search_bottom_sheet.setSkipCollapsed(false)
        search_bottom_sheet.setFitContents(false)
//        search_bottom_sheet.setExpandedOffset(resources.getDimension(R.dimen.peek_search_height).toInt())
        search_bottom_sheet.setPeekHeight(resources.getDimension(R.dimen.peek_search_height).toInt())

        addFilterList()
        addResultList()

        searchQuery.doOnTextChanged { text, start, count, after ->
            addResults(text?.length ?: return@doOnTextChanged)
        }
    }

    private fun addFilterList() {
        filterAdapter.registerPresenter(ColumnPresenter())
        filterList.adapter = filterAdapter

        addFilter(5)
    }

    private fun addFilter(amount: Int) {
        val items = (0 until amount).map {
            PresenterModel(Column("https://lorempixel.com/200/3%02d/".format(it)), uuid = it.toString(), layout = R.layout.item_column)
        }

        filterAdapter.submitList(items)
    }

    private fun addResultList() {
        resultAdapter.registerPresenter(RowPresenter())
        resultsList.adapter = resultAdapter

        addResults(100)
    }

    private fun addResults(amount: Int) {
        val items = (0 until amount).map {
            PresenterModel(Row("https://lorempixel.com/2%02d/300/".format(it), "$it label"), uuid = it.toString(), layout = R.layout.item_icon_with_label)
        }

        resultAdapter.submitList(items)
    }
}