package com.sandg.tastebuds.base

import com.sandg.tastebuds.models.Student

typealias StudentsCompletion = (List<Student>) -> Unit
typealias StudentCompletion = (Student) -> Unit
typealias Completion = () -> Unit