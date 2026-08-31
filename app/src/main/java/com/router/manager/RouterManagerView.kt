package com.router.manager

import android.content.Context
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.BaseAdapter
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView

/**
 * 路由器管理页面（列表），作为 View 嵌入 MainActivity
 */
class RouterManagerView(context: Context) : FrameLayout(context) {

    var onRouterClick: ((RouterStore.Router) -> Unit)? = null
    var onRouterEdit: ((RouterStore.Router) -> Unit)? = null
    var onRouterDelete: ((RouterStore.Router) -> Unit)? = null

    private val listView: ListView
    private val adapter: RouterListAdapter
    private var routers: MutableList<RouterStore.Router> = mutableListOf()

    init {
        listView = ListView(context)
        adapter = RouterListAdapter()
        listView.adapter = adapter
        addView(
            listView,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        )

        listView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            onRouterClick?.invoke(routers[position])
        }

        listView.onItemLongClickListener =
            AdapterView.OnItemLongClickListener { _, _, position, _ ->
                onRouterDelete?.invoke(routers[position])
                true
            }
    }

    fun refresh() {
        routers = RouterStore.loadRouters(context)
        adapter.notifyDataSetChanged()
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    inner class RouterListAdapter : BaseAdapter() {
        override fun getCount() = routers.size
        override fun getItem(position: Int) = routers[position]
        override fun getItemId(position: Int) = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val router = routers[position]

            val layout = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(16), dp(12), dp(16), dp(12))
            }

            val icon = ImageView(context).apply {
                val size = dp(44)
                layoutParams = LinearLayout.LayoutParams(size, size).apply {
                    marginEnd = dp(14)
                }
                setImageDrawable(RouterStore.getIconDrawable(context, router))
            }

            val textLayout = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
                )
            }

            val nameText = TextView(context).apply {
                text = router.name
                textSize = 16f
                setTextColor(0xFF222222.toInt())
                setTypeface(null, Typeface.BOLD)
            }

            val urlText = TextView(context).apply {
                text = router.url
                textSize = 12f
                setTextColor(0xFF888888.toInt())
            }

            textLayout.addView(nameText)
            textLayout.addView(urlText)

            val editBtn = ImageView(context).apply {
                val size = dp(36)
                layoutParams = LinearLayout.LayoutParams(size, size)
                setImageResource(R.drawable.ic_edit)
                setColorFilter(0xFF666666.toInt(), PorterDuff.Mode.SRC_ATOP)
                setPadding(dp(6), dp(6), dp(6), dp(6))
                setOnClickListener {
                    onRouterEdit?.invoke(router)
                }
            }

            layout.addView(icon)
            layout.addView(textLayout)
            layout.addView(editBtn)

            return layout
        }
    }
}
