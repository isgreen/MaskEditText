package br.com.isgreen.maskedittext

import android.content.Context
import android.text.*
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatEditText

/**
 * Created by Éverdes Soares on 6/23/18.
 */

class MaskEditText : AppCompatEditText {

    private var mCurrentMask: String = ""
    private val mMasks: MutableList<String> by lazy { mutableListOf<String>() }

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

        mCurrentMask = if (mMasks.isNotEmpty()) mMasks[0] else ""

        tpArray.recycle()

        if (mMasks.size > 0) {
            this.filters = arrayOf<InputFilter>(InputFilter.LengthFilter(mMasks.last().length))
        }

        this.inputType = InputType.TYPE_CLASS_NUMBER
        this.addTextChangedListener(onTextChange)
    }

    private fun getNextMask(): String {
        mMasks.forEach {
            if (it.length > mCurrentMask.length) {
                return it
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

    fun getRawText(): String {
        if (mCurrentMask == "") {
            return text.toString()
        }

        val symbols = mCurrentMask.replace("#", "")

        return text.toString().replace(Regex("[$symbols]"), "")
                .replace(Regex("\\s+"), "")
    }

    private fun getMask(textLength: Int): String {
        mMasks.forEach { mask ->
            val maskLength = mask.count { c -> c == '#' }

            if (textLength == maskLength) {
                return mask
            }
        }

        return ""
    }

    fun setText(text: String?) {
        text?.let {
            mCurrentMask = getMask(it.length)

            for (i in 0 until mCurrentMask.length) {
                if (it.length > i) {
                    val newText = super.getText().toString()
                    super.setText(String.format("%s%s", newText, text.subSequence(i, i + 1)))
                }
            }
        }
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
                        val rawText = getRawText()

                        mCurrentMask = getNextMask()

                        changeMask(rawText)
                    } else {
                        applyMask(s.toString())
                    }
                }
                count < before -> {
                    if (mMasks.size > 1
                            && mMasks.indexOf(mCurrentMask) > 0
                            && s.length < mCurrentMask.length) {
                        val rawText = getRawText()
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
}
