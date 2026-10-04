package com.homebrain.app

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.text.TextWatcher
import android.text.Editable
import android.view.Gravity
import android.view.View
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : Activity() {
    data class Item(val name: String, val category: String, val price: String, val warranty: String)
    private val items = mutableListOf<Item>()
    private val prefs by lazy { getSharedPreferences("homebrain", MODE_PRIVATE) }
    private val bg = Color.rgb(246, 248, 247)
    private val ink = Color.rgb(28, 43, 52)
    private val muted = Color.rgb(111, 127, 136)
    private val teal = Color.rgb(22, 130, 117)
    private lateinit var page: LinearLayout
    private lateinit var list: LinearLayout
    private lateinit var search: EditText
    private var categoryFilter = "הכול"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        load()
        draw()
    }

    private fun d(n: Int) = (n * resources.displayMetrics.density).toInt()
    private fun panel(color: Int, radius: Int = 18, border: Int = 0) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = d(radius).toFloat()
        if (border != 0) setStroke(d(1), border)
    }
    private fun label(s: String, size: Float, color: Int = ink, bold: Boolean = false) = TextView(this).apply {
        text = s
        textSize = size
        setTextColor(color)
        gravity = Gravity.RIGHT
        textDirection = View.TEXT_DIRECTION_RTL
        layoutDirection = View.LAYOUT_DIRECTION_RTL
        if (bold) setTypeface(typeface, Typeface.BOLD)
    }
    private fun spacing(n: Int) {
        page.addView(Space(this), LinearLayout.LayoutParams(1, d(n)))
    }

    private fun draw() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }
        val scroll = ScrollView(this)
        page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(d(20), d(20), d(20), d(24))
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }
        scroll.addView(page)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        val nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.WHITE)
            setPadding(d(8), d(10), d(8), d(10))
        }
        listOf("⌂  הבית", "▦  הפריטים", "◷  תזכורות").forEachIndexed { i, s ->
            val t = label(s, 13f, if (i == 0) teal else muted, i == 0).apply {
                gravity = Gravity.CENTER
                setPadding(d(5), d(8), d(5), d(8))
            }
            t.setOnClickListener {
                if (i == 2) info("תזכורות", "בגרסה הבאה נוסיף תזכורות תחזוקה ואחריות. בינתיים אפשר לשמור תאריך אחריות בכל פריט.")
                else { categoryFilter = "הכול"; draw() }
            }
            nav.addView(t, LinearLayout.LayoutParams(0, -2, 1f))
        }
        root.addView(nav)
        setContentView(root)

        val header = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val brand = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        brand.addView(label("HomeBrain", 23f, ink, true))
        brand.addView(label("הבית שלך, מסודר בראש", 12f, muted))
        header.addView(brand, LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(TextView(this).apply {
            text = "⌂"; textSize = 25f; gravity = Gravity.CENTER; setTextColor(Color.WHITE)
            background = panel(teal, 16)
            layoutParams = LinearLayout.LayoutParams(d(50), d(50))
        })
        page.addView(header)
        spacing(24)
        page.addView(label("שלום! 👋", 27f, ink, true))
        page.addView(label("כל מה שחשוב בבית — במקום אחד.", 15f, muted).apply { setPadding(0, d(5), 0, 0) })
        spacing(18)

        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(d(20), d(20), d(20), d(20))
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.rgb(22, 130, 117), Color.rgb(20, 91, 96))).apply { cornerRadius = d(24).toFloat() }
        }
        hero.addView(label("הבית שלך זוכר הכול", 21f, Color.WHITE, true))
        hero.addView(label("מוצרים, מחירים ואחריות — מסודרים ונגישים.", 14f, Color.rgb(225, 245, 240)).apply { setPadding(0, d(8), 0, 0) })
        hero.addView(TextView(this).apply {
            text = "＋  הוספת פריט"
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(teal)
            setTypeface(typeface, Typeface.BOLD)
            background = panel(Color.WHITE, 14)
            setPadding(d(14), d(13), d(14), d(13))
            setOnClickListener { addDialog() }
        }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = d(18) })
        page.addView(hero)
        spacing(18)

        val stats = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        stats.addView(stat(items.size.toString(), "פריטים בבית"), LinearLayout.LayoutParams(0, d(92), 1f))
        stats.addView(Space(this), LinearLayout.LayoutParams(d(10), 1))
        stats.addView(stat(items.count { it.price.isNotBlank() }.toString(), "עם מחיר שמור"), LinearLayout.LayoutParams(0, d(92), 1f))
        page.addView(stats)
        spacing(24)

        val section = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        section.addView(label("הפריטים שלי", 20f, ink, true), LinearLayout.LayoutParams(0, -2, 1f))
        section.addView(label("＋ הוספה", 13f, teal, true).apply { setOnClickListener { addDialog() } })
        page.addView(section)
        spacing(12)

        search = EditText(this).apply {
            hint = "חיפוש פריט או קטגוריה"
            textSize = 14f
            singleLine = true
            setTextColor(ink)
            setHintTextColor(muted)
            background = panel(Color.WHITE, 14, Color.rgb(226, 232, 230))
            setPadding(d(14), d(10), d(14), d(10))
        }
        page.addView(search, LinearLayout.LayoutParams(-1, d(50)))
        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { drawItems() }
            override fun afterTextChanged(s: Editable?) {}
        })
        spacing(10)

        val chips = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.RIGHT }
        listOf("הכול", "חשמל", "מטבח", "ריהוט", "אחר").forEach { c ->
            val chip = TextView(this).apply {
                text = c; textSize = 12f; gravity = Gravity.CENTER
                setTextColor(if (categoryFilter == c) Color.WHITE else muted)
                background = panel(if (categoryFilter == c) teal else Color.WHITE, 18)
                setPadding(d(10), d(8), d(10), d(8))
                setOnClickListener { categoryFilter = c; draw() }
            }
            chips.addView(chip, LinearLayout.LayoutParams(-2, -2).apply { marginStart = d(5) })
        }
        page.addView(chips)
        spacing(12)
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        page.addView(list)
        drawItems()
    }

    private fun stat(number: String, caption: String) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        background = panel(Color.WHITE, 18, Color.rgb(233, 238, 236))
        setPadding(d(14), d(12), d(14), d(12))
        addView(label("✦  $number", 22f, teal, true))
        addView(label(caption, 12f, muted))
    }

    private fun drawItems() {
        if (!::list.isInitialized) return
        list.removeAllViews()
        val q = if (::search.isInitialized) search.text.toString().trim() else ""
        val shown = items.filter {
            (categoryFilter == "הכול" || it.category == categoryFilter) &&
                (q.isBlank() || it.name.contains(q, true) || it.category.contains(q, true))
        }
        if (shown.isEmpty()) {
            val empty = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                background = panel(Color.WHITE, 18, Color.rgb(233, 238, 236))
                setPadding(d(18), d(24), d(18), d(24))
            }
            empty.addView(label(if (items.isEmpty()) "עדיין אין פריטים שמורים" else "לא נמצאו פריטים", 16f, ink, true).apply { gravity = Gravity.CENTER })
            empty.addView(label(if (items.isEmpty()) "הוסיפו מוצר כדי להתחיל לבנות את הזיכרון של הבית." else "נסו לשנות את החיפוש או הקטגוריה.", 13f, muted).apply {
                gravity = Gravity.CENTER; setPadding(0, d(8), 0, 0)
            })
            list.addView(empty)
            return
        }
        shown.forEach { item ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                background = panel(Color.WHITE, 18, Color.rgb(233, 238, 236))
                setPadding(d(16), d(14), d(16), d(14))
            }
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
            val details = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            details.addView(label(item.name, 16f, ink, true))
            details.addView(label(item.category + if (item.price.isNotBlank()) "  ·  ₪" + item.price else "", 12f, muted).apply { setPadding(0, d(4), 0, 0) })
            row.addView(details, LinearLayout.LayoutParams(0, -2, 1f))
            row.addView(TextView(this).apply {
                text = "⌂"; textSize = 20f; gravity = Gravity.CENTER; setTextColor(teal)
                background = panel(Color.rgb(230, 244, 240), 13)
                layoutParams = LinearLayout.LayoutParams(d(44), d(44))
            })
            card.addView(row)
            if (item.warranty.isNotBlank()) card.addView(label("🛡  אחריות: " + item.warranty, 12f, muted).apply { setPadding(0, d(10), 0, 0) })
            card.addView(label("הסרה", 12f, Color.rgb(176, 76, 76)).apply {
                setPadding(0, d(10), 0, 0)
                setOnClickListener {
                    AlertDialog.Builder(this@MainActivity).setTitle("להסיר את " + item.name + "?")
                        .setMessage("הפריט יוסר מהרשימה המקומית.")
                        .setNegativeButton("ביטול", null)
                        .setPositiveButton("הסרה") { _, _ -> items.remove(item); save(); draw() }.show()
                }
            })
            list.addView(card, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = d(10) })
        }
    }

    private fun addDialog() {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(d(20), d(8), d(20), 0) }
        fun field(h: String, type: Int = InputType.TYPE_CLASS_TEXT) = EditText(this).apply {
            hint = h; inputType = type; textSize = 15f; background = panel(bg, 12); setPadding(d(12), d(10), d(12), d(10))
        }
        val name = field("שם הפריט *")
        val category = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, listOf("אחר", "חשמל", "מטבח", "ריהוט"))
        }
        val price = field("מחיר בש״ח (לא חובה)", InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL)
        val warranty = field("תאריך אחריות או הערה")
        box.addView(name); box.addView(category); box.addView(price); box.addView(warranty)
        AlertDialog.Builder(this).setTitle("פריט חדש").setMessage("שמרו את הפרטים כדי למצוא אותם בקלות בהמשך.")
            .setView(box).setNegativeButton("ביטול", null).setPositiveButton("שמירת פריט") { _, _ ->
                val n = name.text.toString().trim()
                if (n.isNotBlank()) {
                    items.add(0, Item(n, category.selectedItem.toString(), price.text.toString().trim(), warranty.text.toString().trim()))
                    save(); draw()
                    Toast.makeText(this, "הפריט נשמר בהצלחה", Toast.LENGTH_SHORT).show()
                } else Toast.makeText(this, "יש להזין שם פריט", Toast.LENGTH_SHORT).show()
            }.show()
    }

    private fun info(title: String, message: String) {
        AlertDialog.Builder(this).setTitle(title).setMessage(message).setPositiveButton("הבנתי", null).show()
    }
    private fun save() {
        val a = JSONArray()
        items.forEach { a.put(JSONObject().put("name", it.name).put("category", it.category).put("price", it.price).put("warranty", it.warranty)) }
        prefs.edit().putString("items", a.toString()).apply()
    }
    private fun load() {
        try {
            val a = JSONArray(prefs.getString("items", "[]"))
            for (i in 0 until a.length()) {
                val o = a.getJSONObject(i)
                items.add(Item(o.optString("name"), o.optString("category"), o.optString("price"), o.optString("warranty")))
            }
        } catch (_: Exception) { items.clear() }
    }
}
