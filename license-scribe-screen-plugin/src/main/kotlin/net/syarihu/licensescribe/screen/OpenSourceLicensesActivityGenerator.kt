package net.syarihu.licensescribe.screen

import java.io.File

/**
 * Generates OpenSourceLicensesActivity code using View-based UI (no Compose dependency).
 */
class OpenSourceLicensesActivityGenerator {

  fun generate(
    packageName: String,
    licensesClassName: String,
    activityClassName: String,
    outputDir: File,
    resOutputDir: File,
  ) {
    val packageDir = File(outputDir, packageName.replace('.', '/'))
    packageDir.mkdirs()

    val activityFile = File(packageDir, "$activityClassName.kt")
    activityFile.writeText(generateActivityCode(packageName, licensesClassName, activityClassName))

    // Generate layout XML
    val layoutDir = File(resOutputDir, "layout")
    layoutDir.mkdirs()
    val layoutFile = File(layoutDir, "licensescribe_recycler_view.xml")
    layoutFile.writeText(generateLayoutXml())
  }

  /**
   * Generate layout XML for RecyclerView.
   *
   * RecyclerView is defined in XML to enable scrollbar display.
   * Programmatically created RecyclerView cannot show scrollbars properly
   * because ScrollBarDrawable is not initialized from theme.
   */
  private fun generateLayoutXml(): String = """
<?xml version="1.0" encoding="utf-8"?>
<androidx.recyclerview.widget.RecyclerView
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/licensescribe_recycler_view"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:scrollbars="vertical" />
  """.trimIndent()

  private fun generateActivityCode(
    packageName: String,
    licensesClassName: String,
    activityClassName: String,
  ): String = """
package $packageName

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import net.syarihu.licensescribe.LicenseInfo
import $packageName.R

/**
 * Activity that displays a list of open source licenses.
 *
 * Use [start] to launch this activity with optional customization.
 */
class $activityClassName : AppCompatActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    enableEdgeToEdge()
    super.onCreate(savedInstanceState)

    val themeColor = intent.getIntExtra(EXTRA_THEME_COLOR, -1).takeIf { it != -1 }
    val toolbarTitleColor = intent.getIntExtra(EXTRA_TOOLBAR_TITLE_COLOR, -1).takeIf { it != -1 }
    val title = intent.getStringExtra(EXTRA_TITLE) ?: DEFAULT_TITLE

    // Determine if toolbar background is light or dark
    val isLightBackground = themeColor?.let { ColorUtils.calculateLuminance(it) > 0.5 } ?: true

    // Set status bar icon color based on toolbar background brightness
    WindowCompat.getInsetsController(window, window.decorView).apply {
      isAppearanceLightStatusBars = isLightBackground
    }

    // Auto-determine title color if not specified
    val resolvedTitleColor = toolbarTitleColor
      ?: if (isLightBackground) Color.BLACK else Color.WHITE

    val rootLayout = LinearLayout(this).apply {
      orientation = LinearLayout.VERTICAL
      layoutParams = ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT,
      )
    }

    val toolbar = Toolbar(this).apply {
      layoutParams = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT,
      )
      minimumHeight = getActionBarSize()
      setTitle(title)
      themeColor?.let { setBackgroundColor(it) }
      setTitleTextColor(resolvedTitleColor)
    }
    setSupportActionBar(toolbar)
    supportActionBar?.setDisplayHomeAsUpEnabled(true)

    // Tint navigation icon after setting up action bar
    toolbar.navigationIcon?.colorFilter = PorterDuffColorFilter(resolvedTitleColor, PorterDuff.Mode.SRC_IN)

    // Accent color for license text (use theme color if specified, otherwise default)
    val accentColor = themeColor ?: DEFAULT_THEME_COLOR

    // Inflate RecyclerView from XML to get proper scrollbar support
    val recyclerView = LayoutInflater.from(this)
      .inflate(R.layout.licensescribe_recycler_view, null) as RecyclerView
    recyclerView.apply {
      layoutParams = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        0,
        1f,
      )
      layoutManager = LinearLayoutManager(context)
      clipToPadding = false
      adapter = LicenseAdapter(
        accentColor = accentColor,
        onItemClick = { license ->
          license.artifactUrl?.let { url ->
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
          }
        },
        onLicenseClick = { license ->
          license.licenseUrl?.let { url ->
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
          }
        },
      )
    }

    // Handle WindowInsets for edge-to-edge
    ViewCompat.setOnApplyWindowInsetsListener(toolbar) { view, insets ->
      val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
      view.updatePadding(top = systemBars.top)
      insets
    }

    ViewCompat.setOnApplyWindowInsetsListener(recyclerView) { view, insets ->
      val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
      view.updatePadding(bottom = systemBars.bottom)
      insets
    }

    (recyclerView.adapter as LicenseAdapter).submitList($licensesClassName.all)

    rootLayout.addView(toolbar)
    rootLayout.addView(recyclerView)
    setContentView(rootLayout)
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    return when (item.itemId) {
      android.R.id.home -> {
        finish()
        true
      }
      else -> super.onOptionsItemSelected(item)
    }
  }

  private fun getActionBarSize(): Int {
    val typedValue = TypedValue()
    return if (theme.resolveAttribute(android.R.attr.actionBarSize, typedValue, true)) {
      TypedValue.complexToDimensionPixelSize(typedValue.data, resources.displayMetrics)
    } else {
      dpToPx(56)
    }
  }

  private fun dpToPx(dp: Int): Int =
    TypedValue.applyDimension(
      TypedValue.COMPLEX_UNIT_DIP,
      dp.toFloat(),
      resources.displayMetrics,
    ).toInt()

  private class LicenseAdapter(
    private val accentColor: Int,
    private val onItemClick: (LicenseInfo) -> Unit,
    private val onLicenseClick: (LicenseInfo) -> Unit,
  ) : ListAdapter<LicenseInfo, LicenseAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
      ViewHolder(LicenseItemView(parent.context, accentColor))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
      holder.bind(getItem(position), onItemClick, onLicenseClick)
    }

    class ViewHolder(private val licenseItemView: LicenseItemView) : RecyclerView.ViewHolder(licenseItemView) {
      fun bind(
        license: LicenseInfo,
        onItemClick: (LicenseInfo) -> Unit,
        onLicenseClick: (LicenseInfo) -> Unit,
      ) {
        licenseItemView.bind(license, onLicenseClick)
        licenseItemView.setOnClickListener { onItemClick(license) }
      }
    }

    private object DiffCallback : DiffUtil.ItemCallback<LicenseInfo>() {
      override fun areItemsTheSame(oldItem: LicenseInfo, newItem: LicenseInfo): Boolean =
        oldItem.artifactId == newItem.artifactId

      override fun areContentsTheSame(oldItem: LicenseInfo, newItem: LicenseInfo): Boolean =
        oldItem == newItem
    }
  }

  private class LicenseItemView(context: Context, private val accentColor: Int) : LinearLayout(context) {
    private val titleView: TextView
    private val artifactIdView: TextView
    private val copyrightView: TextView
    private val licenseView: TextView

    init {
      orientation = VERTICAL
      layoutParams = MarginLayoutParams(
        LayoutParams.MATCH_PARENT,
        LayoutParams.WRAP_CONTENT,
      ).apply {
        val margin = dpToPx(16)
        val verticalMargin = dpToPx(8)
        setMargins(margin, verticalMargin, margin, verticalMargin)
      }
      val padding = dpToPx(16)
      setPadding(padding, padding, padding, padding)

      // Card background with rounded corners
      val cardBackground = GradientDrawable().apply {
        setColor(CARD_BACKGROUND_COLOR)
        cornerRadius = dpToPx(8).toFloat()
      }

      // Add ripple effect to card
      val rippleColor = ColorStateList.valueOf(RIPPLE_COLOR)
      background = RippleDrawable(rippleColor, cardBackground, cardBackground)
      isClickable = true
      isFocusable = true

      elevation = dpToPx(4).toFloat()

      titleView = TextView(context).apply {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        setTextColor(TEXT_PRIMARY_COLOR)
        maxLines = 1
      }

      artifactIdView = TextView(context).apply {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        setTextColor(TEXT_SECONDARY_COLOR)
        maxLines = 1
      }

      copyrightView = TextView(context).apply {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        setTextColor(TEXT_SECONDARY_COLOR)
      }

      licenseView = TextView(context).apply {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        setTextColor(accentColor)
        layoutParams = LayoutParams(
          LayoutParams.WRAP_CONTENT,
          LayoutParams.WRAP_CONTENT,
        ).apply {
          topMargin = dpToPx(8)
        }
        // Add ripple effect to license text (gray ripple)
        val licenseRippleColor = ColorStateList.valueOf(RIPPLE_COLOR)
        val mask = GradientDrawable().apply {
          setColor(Color.WHITE) // Mask needs a color to define ripple bounds
          cornerRadius = dpToPx(4).toFloat()
        }
        background = RippleDrawable(licenseRippleColor, null, mask)
        val horizontalPadding = dpToPx(4)
        val verticalPadding = dpToPx(2)
        setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding)
        isClickable = true
        isFocusable = true
      }

      addView(titleView)
      addView(artifactIdView)
      addView(copyrightView)
      addView(licenseView)
    }

    fun bind(license: LicenseInfo, onLicenseClick: (LicenseInfo) -> Unit) {
      titleView.text = license.artifactName
      artifactIdView.text = license.artifactId
      copyrightView.apply {
        text = license.copyrightHolders.joinToString(", ")
        visibility = if (license.copyrightHolders.isEmpty()) View.GONE else View.VISIBLE
      }
      licenseView.apply {
        text = license.licenseName
        if (license.licenseUrl != null) {
          setOnClickListener { onLicenseClick(license) }
          isClickable = true
          isFocusable = true
        } else {
          setOnClickListener(null)
          isClickable = false
          isFocusable = false
        }
      }
    }

    private fun dpToPx(dp: Int): Int =
      TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        dp.toFloat(),
        resources.displayMetrics,
      ).toInt()

    companion object {
      private const val CARD_BACKGROUND_COLOR = 0xFFFFFFFF.toInt()
      private const val TEXT_PRIMARY_COLOR = 0xFF212121.toInt()
      private const val TEXT_SECONDARY_COLOR = 0xFF757575.toInt()
      private const val RIPPLE_COLOR = 0x20000000
    }
  }

  companion object {
    private const val EXTRA_THEME_COLOR = "extra_theme_color"
    private const val EXTRA_TOOLBAR_TITLE_COLOR = "extra_toolbar_title_color"
    private const val EXTRA_TITLE = "extra_title"
    private const val DEFAULT_TITLE = "Open Source Licenses"
    private const val DEFAULT_THEME_COLOR = 0xFF6200EE.toInt()

    /**
     * Start the open source licenses activity.
     *
     * @param context The context to start the activity from
     * @param themeColor Optional theme color used for toolbar background and license text (as Color int)
     * @param toolbarTitleColor Optional title text color for the toolbar (as Color int)
     * @param title Optional custom title (default: "Open Source Licenses")
     */
    @JvmStatic
    fun start(
      context: Context,
      themeColor: Int? = null,
      toolbarTitleColor: Int? = null,
      title: String = DEFAULT_TITLE,
    ) {
      val intent = Intent(context, $activityClassName::class.java).apply {
        themeColor?.let { putExtra(EXTRA_THEME_COLOR, it) }
        toolbarTitleColor?.let { putExtra(EXTRA_TOOLBAR_TITLE_COLOR, it) }
        putExtra(EXTRA_TITLE, title)
      }
      context.startActivity(intent)
    }
  }
}
  """.trimIndent()
}
