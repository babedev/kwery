/*
 * Copyright (c) 2015 Andrew O'Malley
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.github.andrewoma.kwery.mapper.listener

import com.github.andrewoma.kwery.core.Session
import com.github.andrewoma.kwery.mapper.Table
import java.util.concurrent.ConcurrentLinkedQueue
import java.lang

public trait Listener {
    fun onEvent(session: Session, events: List<Event>)
}

public open class Event(val table: Table<*, *>, val id: Any)
public data class InsertEvent(table: Table<*, *>, id: Any, val value: Any): Event(table, id)
public data class DeleteEvent(table: Table<*, *>, id: Any, val value: Any?): Event(table, id)
public data class UpdateEvent(table: Table<*, *>, id: Any, val new: Any?, val old: Any?): Event(table, id)

public class PostCommitListener(val handlerFactory: () -> PostCommitEventHandler) : Listener {
    override fun onEvent(session: Session, events: List<Event>) {
        val transaction = session.currentTransaction
        if (transaction == null) return

        val handler = transaction.postCommitHandler(handlerFactory.javaClass.getName(), handlerFactory) as PostCommitEventHandler
        for (event in events) handler.addEvent(event)
    }
}

public abstract class PostCommitEventHandler: () -> Unit {
    protected val events: MutableList<Event> = arrayListOf()

    public open fun supports(event: Event): Boolean = true

    public fun addEvent(event: Event) {
        if (supports(event)) events.add(event)
    }
}