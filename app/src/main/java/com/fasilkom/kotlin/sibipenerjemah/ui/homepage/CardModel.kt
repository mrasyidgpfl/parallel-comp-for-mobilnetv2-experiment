package com.fasilkom.kotlin.sibipenerjemah.ui.homepage

import android.os.Parcel
import android.os.Parcelable

class CardModel : Parcelable {
    var cardName: String
        private set
    var cardDesc: String
        private set
    var cardImg: Int? = null
        private set

    constructor(name: String, desc: String, img: Int?) {
        cardName = name
        cardDesc = desc
        cardImg = img
    }

    protected constructor(`in`: Parcel) {
        cardName = `in`.readString()!!
        cardDesc = `in`.readString()!!
        cardImg = if (`in`.readByte().toInt() == 0) {
            null
        } else {
            `in`.readInt()
        }
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(cardName)
        dest.writeString(cardDesc)
        if (cardImg == null) {
            dest.writeByte(0.toByte())
        } else {
            dest.writeByte(1.toByte())
            dest.writeInt(cardImg!!)
        }
    }

    companion object {
        val CREATOR: Parcelable.Creator<CardModel?> = object : Parcelable.Creator<CardModel?> {
            override fun createFromParcel(`in`: Parcel): CardModel? {
                return CardModel(`in`)
            }

            override fun newArray(size: Int): Array<CardModel?> {
                return arrayOfNulls(size)
            }
        }
    }
}
