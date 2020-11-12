package br.com.isgreen.maskedittext

import android.content.Context
import android.text.*
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatEditText
import java.util.regex.Pattern

/**
 * Created by Éverdes Soares on 6/23/18.
 */

class MaskEditText : AppCompatEditText {

    private var mMaxLength = 0
    private var mIsMaskEnabled = true
    private var mCurrentMask: String = ""
    private val mMasks: MutableList<String> by lazy { mutableListOf<String>() }

    private var mOnTextChangedListener: OnTextChangedListener? = null

    val maxLength: Int
        get() = mMaxLength

    var isMasksEnabled: Boolean
        get() = mIsMaskEnabled
        set(value) {
            setText("")

            if (value) {
                setMaxLength(Int.MAX_VALUE)
            } else {
                setMaxLength(Int.MAX_VALUE)
            }

            mIsMaskEnabled = value
        }

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
            setMaxLength(mMasks.last().length)
        }

        this.addTextChangedListener(onTextChange)
        this.isLongClickable = false
    }

    private fun setMaxLength(length: Int) {
        mMaxLength = length
        this.filters = arrayOf<InputFilter>(InputFilter.LengthFilter(length))
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

    private fun sortMasksAndChangeMaxLength() {
        mMasks.sortBy { it.length }
        setMaxLength(mMasks.last().length)
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

    fun getRawText(): String {
        if (mCurrentMask == "") {
            return text.toString()
        } else if (TextUtils.isEmpty(text?.toString())){
            return ""
        }

        val symbols = mCurrentMask.replace("#", "")

        return text.toString()
                .replace(Regex("[${Pattern.quote(symbols)}]"), "")
                .replace(Regex("\\s+"), "")
    }

    fun setText(text: String?) {
        text?.let {
            mCurrentMask = getMask(it.length)

            for (i in mCurrentMask.indices) {
                if (it.length > i) {
                    val newText = super.getText().toString()
                    super.setText(String.format("%s%s", newText, text.subSequence(i, i + 1)))
                }
            }
        }
    }

    fun addMask(mask: String) {
        mMasks.add(mask)
        sortMasksAndChangeMaxLength()
    }

    fun addMasks(mask: List<String>) {
        mMasks.addAll(mask)
        sortMasksAndChangeMaxLength()
    }

    fun setMasks(mask: List<String>) {
        mMasks.clear()
        mMasks.addAll(mask)
        sortMasksAndChangeMaxLength()
    }

    fun clearMasks() {
        mMasks.clear()
        mCurrentMask = ""
        setMaxLength(Int.MAX_VALUE)
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

            if (mCurrentMask == "" || !mIsMaskEnabled) {
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
                            && s.length == getPreviousMask().length) {
//                            && s.length < mCurrentMask.length) {
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
