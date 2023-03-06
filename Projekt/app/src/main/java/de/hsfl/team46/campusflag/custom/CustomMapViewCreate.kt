package de.hsfl.team46.campusflag.custom


import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import de.hsfl.team46.campusflag.R
import de.hsfl.team46.campusflag.viewmodels.ViewModel


class CustomMapViewCreate(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    lateinit var viewModel: ViewModel

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    var flagColor = Color.GREEN// choose color from API
    private var RADIUS = 10


    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        drawFaceBackground(canvas!!)
        drawFlag(canvas)
    }

    private fun drawFaceBackground(canvas: Canvas) {
        val myImage: Drawable? = ResourcesCompat.getDrawable(
            context.resources,
            R.drawable.campuskarte, null
        )
        canvas.drawBitmap(myImage!!.toBitmap(canvas.width, canvas.height), matrix, null)
    }

    fun drawFlag(canvas: Canvas) {
        paint.style = Paint.Style.FILL
        paint.color = flagColor

        if (viewModel.getPositions().value != null) {
            viewModel.getPositions().value!!.forEach { pt ->

                val y = viewModel.getLatScaling(pt.lat)
                val x = viewModel.getLongScaling(pt.long)

                canvas.drawCircle(x.toFloat(), y.toFloat(), RADIUS.toFloat(), paint)
            }
        }

        if (viewModel.currentLocation.value != null) {
            paint.color = Color.BLUE

            val y = viewModel.getLatScaling(viewModel.currentLocation.value!!.latitude)
            val x = viewModel.getLongScaling(viewModel.currentLocation.value!!.longitude)

            canvas.drawCircle(x.toFloat(), y.toFloat(), RADIUS.toFloat(), paint)
        }
    }
}

