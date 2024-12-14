package com.stm.bledemovisitor.activity.scan.fragment

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.stm.bledemovisitor.R
import com.stm.bledemovisitor.activity.scan.visitor_mode

class AlertDialogFragment : DialogFragment() {

    private var title : String? = null
    private var message : String? = null
    private var listener : DialogListener? = null

    interface DialogListener {
        fun onDialogClosed()
    }

    companion object {
        fun newInstance(title : String, message : String, listener : DialogListener) : AlertDialogFragment {
            val args = Bundle()
            args.putString("title",title)
            args.putString("message", message)

            val fragment = AlertDialogFragment()
            fragment.arguments = args
            fragment.listener = listener
            return fragment
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            title = it.getString("title").toString()
            message = it.getString("message").toString()
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_dialog_alert, container, false) // use your dialog layout
        val dialogTitle = view.findViewById<TextView>(R.id.dialogTitle)
        val dialogMessage = view.findViewById<TextView>(R.id.dialogMessage)
        val closeButton = view.findViewById<Button>(R.id.closeButton)
        val dialogLayout = view.findViewById<LinearLayout>(R.id.dialogLayout)
        val cardView = view.findViewById<androidx.cardview.widget.CardView>(R.id.dialogCard)

        if (title == null || message == null) {
            dismiss()
            return view
        }

        if(visitor_mode){
            dialogLayout.background = ColorDrawable(Color.GRAY)
            cardView.background = ColorDrawable(Color.GRAY)
            cardView.minimumHeight = 400
            dialogLayout.minimumHeight = 400
            closeButton.setBackgroundColor(resources.getColor(R.color.redred))
            closeButton.setTextColor(Color.BLACK)
        }

        //Set Values
        dialogTitle.text = title;
        dialogMessage.text = message;
//        dialogLayout.background = ColorDrawable(Color.TRANSPARENT)
        closeButton.setOnClickListener {
            listener?.onDialogClosed()
            dismiss()
        }

//        dismiss()

        return view

    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)


        return dialog
    }

}