package com.example.mipaint

import android.Manifest
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.MediaScannerConnection
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream


class MainActivity : AppCompatActivity() {

    // Create a ActivityResultLauncer with the intent to open the Gallery
    private val openGalleryLauncher: ActivityResultLauncher<Intent> =
    //register for Activity result  with Activity result contracts and start an activity for the result
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            //Get the result for the lamda
            result ->
            // if the result with RESULT_OK result code and also result data (External content URI) is not empty then
            if (result.resultCode == RESULT_OK && result.data != null) {
                // assign the image URI to the background image view
                val backgroundImage: ImageButton = findViewById(R.id.backgroundIV)
                backgroundImage.setImageURI(result.data?.data)
            }
        }


    //Create a private var for the drawing view
    private var drwingView: DrawingView? = null
    // private global var for selected color pallete ImageButton
    private var mCurrentPaintImageButton: ImageButton? = null
    //variable for the custom progress bar dialog
    var customProgressBarDialog: Dialog? = null

    //Variable for the permission
    private val storageResultLauncer: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            permissions ->
            permissions.entries.forEach {
                val permissionName = it.key
                val isGranted = it.value
                if (isGranted) {
                    if (permissionName == Manifest.permission.READ_EXTERNAL_STORAGE){
                        Toast.makeText(this, "Permission Granted for External Storage", Toast.LENGTH_LONG).show()

                        //Create a intent with a ACTION_PICK which can point the intent to
                        // MediaStore Image External Content Uri
                        val pickIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                        //open the Activity Result launcher to open the gallery
                        openGalleryLauncher.launch(pickIntent)
                    }
                    else {
                        Toast.makeText(this, "Permission granted for save data", Toast.LENGTH_LONG).show()
                    }
                } else {
                    if (permissionName == Manifest.permission.READ_EXTERNAL_STORAGE){
                        Toast.makeText(this, "Permission for External storage is Not Granted",Toast.LENGTH_LONG).show()
                    }else {
                        Toast.makeText(this, "Permission for write external storage is Not Granted",Toast.LENGTH_LONG).show()
                    }
                }
            }

        }




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //set the drawingView with the DrawingView id
        drwingView = findViewById(R.id.drawingView)
        //then set the brush size on that drawing view with help of setSizeForBrush
        drwingView!!.setSizeForBrush(20.toFloat())

        // initialize the Color pallete LinearLayout
        val paintColorLinearLayout = findViewById<LinearLayout>(R.id.paintColorLL)

        //Set the LinearLayout index position as ImageButton for selected_palete to show which color is selected
        mCurrentPaintImageButton = paintColorLinearLayout[3] as ImageButton
        mCurrentPaintImageButton!!.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.pallete_selected))

        // initialize a variable with the brush chooser icon of the main layout
        val brushIb : ImageButton = findViewById(R.id.brushIB)
        //set an onclick listener
        brushIb.setOnClickListener {
            //call the Dialog function
            showBrushChooserDialog()
        }

        val openGalleryBtn: ImageButton = findViewById(R.id.gelleryIB)
        openGalleryBtn.setOnClickListener {
            permissionCheck()
        }

        val undoBtn: ImageButton = findViewById(R.id.undoIB)
        undoBtn.setOnClickListener {
            // call the Undo method
            drwingView!!.clickOnUndo()
        }

        //Save button implementation
        val saveBtn: ImageButton = findViewById(R.id.saveIB)
        saveBtn.setOnClickListener {
            // if isReadStorageAllowed then
            if (isReadStorageAllowed()) {
                showProgressDialog()
                //lifecycleScope is launch to save the file
                lifecycleScope.launch {
                    //First initialize the FrameLayout with its id
                    val containerFL : FrameLayout = findViewById(R.id.drawingViewContainerFL)
                    // then call saveBitmapFile with getBitmapFromView which have the framelayout variable as argument
                    saveBitmapFile(getBitmapFromView(containerFL))
                }
            }
        }
    }

    //create a function to show the Brush chosser Dialog
    fun showBrushChooserDialog() {
        // Create a variable for the Dialog function
        val brushDialog = Dialog(this)
        //set the proper layout file for the Dialog useing setContentView
        brushDialog.setContentView(R.layout.dialog_brush_size)
        //set the title for the Dialog
        brushDialog.setTitle("Brush Dialog: ")
        // Create a variable to initialize the smallButton with the layout small btn id
        val smallBtn : ImageButton = brushDialog.findViewById(R.id.smallBrushIB)
        // set a onClick Listener for the btn
        smallBtn.setOnClickListener (View.OnClickListener {
            // set the brush size for the drawing view
            drwingView?.setSizeForBrush(10.toFloat())
            //Dismiss the Dialog
            brushDialog.dismiss()
        })

        val mediumBtn: ImageButton = brushDialog.findViewById(R.id.mediumBrushIB)
        mediumBtn.setOnClickListener (View.OnClickListener {
            drwingView?.setSizeForBrush(20.toFloat())
            brushDialog.dismiss()

        })

        val largeBtn: ImageButton = brushDialog.findViewById(R.id.largeBrushIB)
        largeBtn.setOnClickListener (View.OnClickListener {
            drwingView?.setSizeForBrush(30.toFloat())
            brushDialog.dismiss()

        })

        brushDialog.show()
        // Show the brush Dialog
    }

    fun paintClicked(view: View){
        //check with if to ensure that user not pressed the same color pellete which is already selected
        //if not selected then
        if (view !== mCurrentPaintImageButton) {
            // initialize the view argument or the selected view with a variable as a ImageButton
            val imageButton = view as ImageButton
            //initialize and create a variable to hold the tag properties of that selected view as a string
            val tag = imageButton.tag.toString()
            //set the drawing view color with the tag value as a string
            drwingView?.setColor(tag)

            // and also set the selected view layout to selected design layout
            imageButton.setImageDrawable(ContextCompat.getDrawable(this,R.drawable.pallete_selected))

            // set the layout pellete normal to the previously selected color pellete or mCurrentPaintImageButton so to say
            mCurrentPaintImageButton?.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.pallete_normal))

            //and also set the selected view to mCurrentPaintImageButton
            mCurrentPaintImageButton = view
        }
    }

    //Create a private fun to ensure that is ReadStorageAllowed or not which return a boolean
    private fun isReadStorageAllowed(): Boolean {
        //Create a variable to hold the ContextCompat checkSelfPermision wiht context and required permission argument
        val result = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
        //return the variable with Permission_Granted value
        return result == PackageManager.PERMISSION_GRANTED
    }

    fun permissionCheck(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)) {
            showRationalDialog("MiPaint requires storage permission", "Sorry you dont have storage permission")
        } else {
            storageResultLauncer.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE))
        }
    }

    private fun showRationalDialog( title: String, message: String){
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle(title)
            .setMessage(message)
            .setPositiveButton("Cancel"){dialog, _ ->
                dialog.dismiss()
            }
        builder.create().show()
    }

    //Create a function to convert view which contain background image and drawing into a bitmap as a sandwich of all
    //fun called getBitmapFromView which requires view as a argument and return a Bitmap
    private fun getBitmapFromView(view: View): Bitmap {
        //create a variable of return Bitmap which can hold width & height of the argument view and
        //with ARGB_8888 config and create the bitmap
        val returnBitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        // create a canvas variable which Create Canvas for the return Bitmap
        val canvas = Canvas(returnBitmap)
        // Create a variable for the background Draw
        val backgroundDrawable = view.background
        //If backgroundDrawable is empty then draw the canvas with white color
        if (backgroundDrawable != null){
        //If not empty then draw background drawable to the canvas
            backgroundDrawable.draw(canvas)
        } else {
            canvas.drawColor(Color.WHITE)
        }
        // Now draw the view with canvas
        view.draw(canvas)
        // return return bitmap
        return returnBitmap
    }

    //TODO Create a suspend function to saveBitmapFile which has a Bitmap Parameter and return a String(Path name for the file)
    private suspend fun saveBitmapFile(mBitmap: Bitmap): String {

        //create a var for result which hold the path name at last
        var result = ""
        //withContext courutine scope with Dispatcher.IO
        withContext(Dispatchers.IO) {
            //if bitmap parameter is not null then try and catch block is process
            if (mBitmap != null) {
                //try block
                try {
                    //Create a var to hold byte array outputstream
                    var byte = ByteArrayOutputStream()
                    //compress the bitmap with CompressFormat.PNG, quality and byteOutputStream argument
                    mBitmap.compress(Bitmap.CompressFormat.PNG, 90, byte)
                    //Create a variable to hold File with all file location details
                    val f = File(externalCacheDir?.absoluteFile.toString() +
                            File.separator + "MiDrawing_" + System.currentTimeMillis()/1000 + ".jpg")
                    //Create a variable to hold fileOutputStream with file which is declare earlier
                    val fo = FileOutputStream(f)
                    //then fileOutputStream write with byteArrayOutputStream
                    fo.write(byte.toByteArray())
                    //then close the fileOutputStream
                    fo.close()
                    //The file absolute path is return as a result.
                    //Set the result variable with file variable and with absolutePath
                    result = f.absolutePath

                    //Create a Toast on the runUiThread to display the file name if result path is not empty else also required
                    runOnUiThread{
                        cancelProgressBarDialog()
                        if (result.isNotEmpty()){
                            Toast.makeText(this@MainActivity, "Save Successfully: $result", Toast.LENGTH_LONG).show()
                            //TODO call the share function here
                            shareImage(result)
                        } else {
                            Toast.makeText(this@MainActivity, "File not Save", Toast.LENGTH_LONG).show()
                        }
                    }
                } catch (e: Exception) {
                    //implement the catch block where set the result to empty string and printStackTrace too
                    result = ""
                    e.printStackTrace()
                }
            }
        }
        //return the result for function to process
        return result
    }

    private fun showProgressDialog(){
        customProgressBarDialog = Dialog(this@MainActivity)
        customProgressBarDialog?.setContentView(R.layout.custom_progress_bar_dialog)
        customProgressBarDialog?.show()
    }

    private fun cancelProgressBarDialog(){
        if (customProgressBarDialog != null){
            customProgressBarDialog?.dismiss()
            customProgressBarDialog = null
        }
    }

    //TODO Create a function to perform share function
    //function need a String parameter which hold the uri of the image
    private fun shareImage(result: String) {
        //scan the file which want to share with MediaScannerConnection
        MediaScannerConnection.scanFile(this@MainActivity, arrayOf(result), null) {
            //this scanFile return uri and path
            path, uri ->
            //Create a variable for intent
            val shareIntent = Intent()
            //set the action for the intent as ACTION_SEND
            shareIntent.action = Intent.ACTION_SEND
            //putExtra into this intent with Intent.EXTRA_STREAM and uri
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
            //set the type for the intent as image/png
            shareIntent.type = "image/png"
            //start the activity with Intent.createChooser and have the intent with a title as a paremeter
            startActivity(Intent.createChooser(shareIntent, "Share"))
        }
    }

}