package de.hsfl.team46.campusflag.custom

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import de.hsfl.team46.campusflag.R
import de.hsfl.team46.campusflag.viewmodels.ViewModel


class CustomMapViewGame(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    lateinit var viewModel: ViewModel

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
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

    private fun drawFlag(canvas: Canvas) {
        paint.style = Paint.Style.FILL

        if (viewModel.points.value != null) {
            viewModel.points.value!!.forEach { pt ->
                Log.d("pt", pt.toString())

                paint.color = viewModel.getColorFromApi(pt.team!!.toInt())

                val y = viewModel.getLatScaling(pt.lat)
                val x = viewModel.getLongScaling(pt.long)

                canvas.drawCircle(x.toFloat(), y.toFloat(), RADIUS.toFloat(), paint)
            }
        }

        if (viewModel.currentLocation.value != null) {
            paint.color = Color.GREEN

            val y = viewModel.getLatScaling(viewModel.currentLocation.value!!.latitude)
            val x = viewModel.getLongScaling(viewModel.currentLocation.value!!.longitude)

            canvas.drawCircle(x.toFloat(), y.toFloat(), RADIUS.toFloat(), paint)
        }
    }
}

