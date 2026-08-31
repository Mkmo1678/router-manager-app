package com.router.manager

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import java.io.File

/**
 * 路由器管理页面：全屏展示所有路由器，支持切换、编辑、删除、添加
 */
class RouterListActivity : Activity() {

    private lateinit var listView: ListView
    private lateinit var adapter: RouterAdapter
    private var routers: MutableList<RouterStore.Router> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        actionBar?.title = "路由器管理"
        actionBar?.setDisplayHomeAsUpEnabled(true)

        routers = RouterStore.loadRouters(this)

        listView = ListView(this)
        adapter = RouterAdapter()
        listView.adapter = adapter
        setContentView(listView)

        // 点击切换
        listView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val router = routers[position]
            RouterStore.setCurrentRouterId(this, router.id)
            Toast.makeText(this, "已切换到「${router.name}」", Toast.LENGTH_SHORT).show()
            finish()
        }

        // 长按删除
        listView.onItemLongClickListener = AdapterView.OnItemLongClickListener { _, _, position, _ ->
            showDeleteDialog(position)
            true
        }
    }

    override fun onResume() {
        super.onResume()
        routers = RouterStore.loadRouters(this)
        adapter.notifyDataSetChanged()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menu?.add(0, 1, 0, "添加")?.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            1 -> {
                RouterEditor.showEditDialog(this, null, true) { router ->
                    val list = RouterStore.loadRouters(this)
                    list.add(router)
                    RouterStore.saveRouters(this, list)
                    RouterStore.setCurrentRouterId(this, router.id)
                    routers = list
                    adapter.notifyDataSetChanged()
                    Toast.makeText(this, "已添加「${router.name}」", Toast.LENGTH_SHORT).show()
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        RouterEditor.handleActivityResult(this, requestCode, resultCode, data)
    }

    private fun showDeleteDialog(position: Int) {
        val router = routers[position]
        AlertDialog.Builder(this)
            .setTitle("删除路由器")
            .setMessage("确定要删除「${router.name}」吗？")
            .setPositiveButton("删除") { _, _ ->
                router.customIconPath?.let { File(it).delete() }
                routers.removeAt(position)
                RouterStore.saveRouters(this, routers)
                // 如果删的是当前使用的，切到第一个
                val currentId = RouterStore.getCurrentRouterId(this)
                if (currentId == router.id) {
                    RouterStore.setCurrentRouterId(this, routers.firstOrNull()?.id)
                }
                adapter.notifyDataSetChanged()
                Toast.makeText(this, "已删除", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    // ─── 列表适配器 ──────────────────────────────────────

    inner class RouterAdapter : BaseAdapter() {
        override fun getCount() = routers.size
        override fun getItem(position: Int) = routers[position]
        override fun getItemId(position: Int) = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val router = routers[position]
            val currentId = RouterStore.getCurrentRouterId(this@RouterListActivity)

            val layout = LinearLayout(this@RouterListActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(16), dp(12), dp(16), dp(12))
            }

            val icon = ImageView(this@RouterListActivity).apply {
                val size = dp(44)
                layoutParams = LinearLayout.LayoutParams(size, size).apply {
                    marginEnd = dp(14)
                }
                setImageDrawable(RouterStore.getIconDrawable(this@RouterListActivity, router))
            }

            val textLayout = LinearLayout(this@RouterListActivity).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
                )
            }

            val nameText = TextView(this@RouterListActivity).apply {
                text = if (router.id == currentId) "✓ ${router.name}" else router.name
                textSize = 16f
                setTextColor(0xFF222222.toInt())
                setTypeface(null, Typeface.BOLD)
            }

            val urlText = TextView(this@RouterListActivity).apply {
                text = router.url
                textSize = 12f
                setTextColor(0xFF888888.toInt())
            }

            textLayout.addView(nameText)
            textLayout.addView(urlText)

            val editBtn = ImageView(this@RouterListActivity).apply {
                val size = dp(36)
                layoutParams = LinearLayout.LayoutParams(size, size)
                setImageResource(R.drawable.ic_edit)
                setColorFilter(0xFF666666.toInt(), PorterDuff.Mode.SRC_ATOP)
                setPadding(dp(6), dp(6), dp(6), dp(6))
                setOnClickListener {
                    RouterEditor.showEditDialog(this@RouterListActivity, router, true) { updated ->
                        val list = RouterStore.loadRouters(this@RouterListActivity)
                        val idx = list.indexOfFirst { it.id == router.id }
                        if (idx >= 0) list[idx] = updated
                        RouterStore.saveRouters(this@RouterListActivity, list)
                        routers = list
                        adapter.notifyDataSetChanged()
                        Toast.makeText(this@RouterListActivity, "已保存", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            layout.addView(icon)
            layout.addView(textLayout)
            layout.addView(editBtn)

            return layout
        }
    }
}
