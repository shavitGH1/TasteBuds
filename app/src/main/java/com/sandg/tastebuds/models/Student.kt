package com.sandg.tastebuds.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Student(

    @PrimaryKey
    val id: String,

    val name: String,
    var isPresent: Boolean,
    val avatarUrlString: String?
) {

    companion object {

        fun fromJson(json: Map<String, Any?>): Student {
            val id = json["id"] as String
            val name = json["name"] as String
            val isPresent = json["isPresent"] as Boolean
            val avatarUrlString = json["avatarUrlString"] as String?

            return Student(
                id = id,
                name = name,
                isPresent = isPresent,
                avatarUrlString = avatarUrlString
            )
        }
    }

    val toJson: Map<String, Any?>
        get() = hashMapOf(
            "id" to id,
            "name" to name,
            "isPresent" to isPresent,
            "avatarUrlString" to avatarUrlString
        )
}