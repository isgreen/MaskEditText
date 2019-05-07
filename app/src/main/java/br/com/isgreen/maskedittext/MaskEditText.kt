package br.com.isgreen.maskedittext

import android.content.Context
import android.text.Editable
import android.text.InputFilter
import android.text.TextUtils
import android.text.TextWatcher
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatEditText

/**
 * Created by Éverdes Soares on 6/23/18.
 */

class MaskEditText : AppCompatEditText {

    private var mMaxLength = 0
    private var mCurrentMask: String = ""
    private val mMasks: MutableList<String> by lazy { mutableListOf<String>() }

    private var mOnTextChangedListener: OnTextChangedListener? = null

    constructor(context: Context) : super(context) {
        init(context, null)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(context, attrs)
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        val tpArray = context.obtainStyledAttributes(attrs, R.styleable.MaskEditText)
        val mask = tpArray.getText(R.styleable.MaskEditText_mask) as? String

        if (mask != null) {
            mMasks.addAll(mask.split("|"))
        }

        mMasks.sortBy { it.length }
        mMasks.removeAll { it == "" }

        mCurrentMask = getNextMask()

        tpArray.recycle()

        if (mMasks.size > 0) {
            setMaxLength(mMasks.last().length)
        }

        this.addTextChangedListener(onTextChange)
        this.isLongClickable = false
    }

    private fun setMaxLength(length: Int) {
        mMaxLength = length
        this.filters = arrayOf<InputFilter>(InputFilter.LengthFilter(length))
    }

    val maxLength: Int
        get() = mMaxLength

    private fun getNextMask(): String {
        mMasks.forEach { mask ->
            if (text.toString().length >= mask.length) {
                return mask
            }
        }

        return ""
    }

    private fun getPreviousMask(): String {
        return mMasks[mMasks.indexOf(mCurrentMask) - 1]
    }

    fun addMask(mask: String) {
        mMasks.add(mask)
        mMasks.sortBy { it.length }
        this.filters = arrayOf<InputFilter>(InputFilter.LengthFilter(mMasks.last().length))
    }

    fun getTextWithoutMask(): String {
        if (mCurrentMask == "") {
            return text.toString()
        }

        val symbols = mCurrentMask.replace("#", "")

        return text.toString().replace(Regex("[$symbols]"), "")
                .replace(Regex("\\s+"), "")
    }

    fun setText(text: String?) {
        text?.let {
            for (i in 0 until mCurrentMask.length) {
                if (it.length > i) {
                    val newText = super.getText().toString()
                    super.setText(String.format("%s%s", newText, text.subSequence(i, i + 1)))
                }
            }
        }
    }

    fun setOnTextChangedListener(onTextChangedListener: OnTextChangedListener) {
        mOnTextChangedListener = onTextChangedListener
    }

    private val onTextChange = object : TextWatcher {

        private var isUpdating: Boolean = false

        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

        }

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            if (isUpdating) {
                isUpdating = false
                return
            }

            if (mCurrentMask == "") {
                return
            }

            val text = when {
                count > before -> {
                    if (s.length > mCurrentMask.length) {
                        mCurrentMask = getNextMask()

                        val rawText = getTextWithoutMask()

                        changeMask(rawText)
                    } else {
                        applyMask(s.toString())
                    }
                }
                count < before -> {
                    if (mMasks.size > 1
                            && mMasks.indexOf(mCurrentMask) > 0
                            && s.length == getPreviousMask().length) {
                        val rawText = getTextWithoutMask()
                        mCurrentMask = getPreviousMask()

                        changeMask(rawText)
                    } else {
                        removeMask(s.toString())
                    }
                }
                else -> s.toString()
            }

            isUpdating = true

            super@MaskEditText.setText(text)
            super@MaskEditText.setSelection(text.length)

            mOnTextChangedListener?.onTextChanged(
                    !TextUtils.isEmpty(text) && text.length == maxLength)
        }

        private fun applyMask(text: String): String {
            var newText = text
            val position = text.lastIndex
            val caracter = mCurrentMask[position]

            if (caracter != '#') {
                newText = newText.substring(0, position) +
                        mCurrentMask.substring(position, mCurrentMask.indexOf('#', position)) +
                        newText.substring(position, position + 1)
            }

            return newText
        }

        private fun removeMask(text: String): String {
            var newText = text
            val position = text.lastIndex

            if (position > -1) {
                val caracter = mCurrentMask[position]

                if (caracter != '#') {
                    newText = newText.substring(0, position)
                }
            }

            return newText
        }

        private fun changeMask(text: String): String {
            var newText = text

            for (i in 0..text.length) {
                val caracter = mCurrentMask[i]

                if (caracter != '#') {
                    newText = newText.substring(0, i) + caracter + newText.substring(i, newText.length)
                }
            }

            return newText
        }

        override fun afterTextChanged(s: Editable) {

        }
    }

    interface OnTextChangedListener {

        fun onTextChanged(filled: Boolean)

    }
}
