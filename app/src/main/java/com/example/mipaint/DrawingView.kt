package com.example.mipaint

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View

class DrawingView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    private var mDrawPath : CustomPath? = null
    private var mCanvasBitmap : Bitmap? = null
    private var mDrawPaint: Paint? = null
    private var mCanvasPaint: Paint? = null
    private var mBrushSize: Float = 0.toFloat()
    private var color = Color.BLACK
    private var canvas: Canvas? = null
    //  Create a variable to hold ArrayList of Custom Path
    private var mPath = ArrayList<CustomPath>()
    // Create a variable for undoPath to hold the ArrayList of Custom Path
    private var mUndoPath = ArrayList<CustomPath>()

    init {
        setUpDrawing()
    }

    // Create a method for undoPath when click on undo
    //This is done by store the last mPath or drawing onto the mUndoPath
    fun clickOnUndo() {
        //For this first check is the mPath size is greater than 0 or is there any drawing on the drawing view
        if (mPath.size > 0) {
            //If it is than add the last mPath to the mUndoPath for this first remove the last entry from the mPath then add to mUndoPath
            mUndoPath.add(mPath.removeAt(mPath.size - 1))
            // call Invalidate() to recall the onDraw method to draw again
            invalidate()
        }
    }

    private fun setUpDrawing(){
        mDrawPaint = Paint()
        mDrawPath = CustomPath(color, mBrushSize)
        mDrawPaint!!.color = color
        mDrawPaint!!.style = Paint.Style.STROKE
        mDrawPaint!!.strokeJoin = Paint.Join.ROUND
        mDrawPaint!!.strokeCap = Paint.Cap.ROUND
        mCanvasPaint = Paint(Paint.DITHER_FLAG)
        //mBrushSize = 20.toFloat()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mCanvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        canvas = Canvas(mCanvasBitmap!!)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        //set the canvas for onDraw
        //set the drawBitmap for the canvas
        //canvas.drawBitmap(mCanvasBitmap!!, 0f, 0f, mCanvasPaint)
        mCanvasBitmap?.let {
            canvas.drawBitmap(it, 0f,   0f, mCanvasPaint)
        }

        // for loop for each and every CustomPath of the CustomPath ArrayList
        for (path in mPath) {
            //Set up the stroke width and color for the path
            mDrawPaint!!.strokeWidth = path.brushTickness
            mDrawPaint!!.color = path.color
            //Then draw the path with each and every path from the arrayList
            canvas.drawPath(path, mDrawPaint!!)
        }

        //Then draw the path with each and every path from the arrayList

        //if the mDrawPath is not empty
        if (!mDrawPath!!.isEmpty) {
            //then set the strokeWidth for the mDrawPaint with mDrawPath brushThickness
            mDrawPaint!!.strokeWidth = mDrawPath!!.brushTickness
            //Set the color of the drawPath
            mDrawPaint!!.color = mDrawPath!!.color
            //DrawPath for the canvas with drawPath and Paint
            canvas.drawPath(mDrawPath!!, mDrawPaint!!)
        }
    }

    //Draw the canvas when we touch the screen and this is implemented by
    //override onTouchEvent

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        //Declare two variable for touchX and touchY with event.x and event.y
        val touchX = event?.x
        val touchY = event?.y

        //According to touch event do something
        // when event action is

        when(event?.action) {
            //Action_Down or with a touch
                MotionEvent.ACTION_DOWN -> {
                    //set the mDrawPath.color
                    mDrawPath!!.color = color
                    //set the brushThickness
                    mDrawPath!!.brushTickness = mBrushSize
                    //reset the mDrawPath
                    mDrawPath!!.reset()
                    //and then moveTo touchX and touchY for mDrawPath
                    if (touchX != null) {
                        if (touchY != null) {
                            mDrawPath!!.moveTo(touchX, touchY)
                        }
                    }
                }

            //Action_Move or when touch and drag the touch
            MotionEvent.ACTION_MOVE -> {
                //mDrawPath set the lineTo with touchX and touchY
                if (touchX != null) {
                    if (touchY != null) {
                        mDrawPath!!.lineTo(touchX, touchY)
                    }
                }
            }

            //Action_Up set the mDrawPath with customPath
            MotionEvent.ACTION_UP -> {
                // add the Custom path on CustomPath ArrayList
                mPath.add(mDrawPath!!)
                mDrawPath = CustomPath(color, mBrushSize)
            }
            //When rest of all motion event is trigger then do nothing
            else -> return false
        }
        invalidate()
        return true
    }

    //Create a public function to set the brushSize with newSize perameter
    fun setSizeForBrush(newSize: Float){
        //set the brushSize with TypeValue.applyDimension so that the brush size is adjust according to the screen size of the device
        mBrushSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, newSize, resources.displayMetrics)
        //set the newBrush size in the mDrawPaint strokeWidth
        mDrawPaint!!.strokeWidth = mBrushSize

    }

    // Create a function to set color for the paint with a argument of newColor as a string
    fun setColor(newColor: String){
    // set the color with help of parseColor
        color = Color.parseColor(newColor)
    // set the mDrawPaint color with the parse color
        mDrawPaint!!.color =  color
    }


    inner internal class CustomPath(var color: Int, var brushTickness: Float) : Path() {

    }
}