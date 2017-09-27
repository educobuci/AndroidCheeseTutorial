/*
 * Copyright (c) 2017 Razeware LLC
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
 * Notwithstanding the foregoing, you may not use, copy, modify, merge, publish,
 * distribute, sublicense, create a derivative work, and/or sell copies of the
 * Software in any work that is designed, intended, or marketed for pedagogical or
 * instructional purposes related to programming, coding, application development,
 * or information technology.  Permission for such use, copying, modification,
 * merger, publication, distribution, sublicensing, creation of derivative works,
 * or sale is expressly withheld.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.raywenderlich.cheesefinder

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_cheeses.*
import java.util.concurrent.TimeUnit

class CheeseActivity : BaseSearchActivity() {
    private val disposables = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val buttonObservable = createButtonClickObservable()
        val textChangeObservable = createTextChangeObservable()
        val searchTextObservable = Observable.merge(buttonObservable, textChangeObservable)
        disposables.add(
            searchTextObservable
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnNext { showProgress() }
                    .observeOn(Schedulers.io())
                    .map { query -> cheeseSearchEngine.search(query) }
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { result ->
                        hideProgress()
                        showResult(result)
                    }
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        disposables.clear()
    }

    private fun createButtonClickObservable(): Observable<String> {
        return Observable.create { emitter ->
            searchButton.setOnClickListener {
                emitter.onNext(queryEditText.text.toString())
            }
            emitter.setCancellable {
                searchButton.setOnClickListener(null)
            }
        }
    }

    private fun createTextChangeObservable(): Observable<String> {
        val textObservable = Observable.create<String> { emitter ->
            var textWatcher = object: TextWatcher {
                override fun afterTextChanged(s: Editable?) = Unit
                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) = Unit
                override fun onTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                    s?.toString()?.let { emitter.onNext(it) }
                }
            }
            queryEditText.addTextChangedListener(textWatcher)
            emitter.setCancellable {
                queryEditText.removeTextChangedListener(textWatcher)
            }
        }
        return textObservable
                .filter { it.length >= 2 }
                .debounce(1000, TimeUnit.MILLISECONDS)

    }
}