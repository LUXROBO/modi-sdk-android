package com.luxrobo.sample.modidemo.sample_modi_demo

import com.polidea.rxandroidble2.scan.ScanResult
import rx.Observable
import rx.subjects.PublishSubject

class Singleton {

    private var scanResultObserverSubject       : PublishSubject<ScanResult?> = PublishSubject.create()
    var         scanResultObserver              : Observable<ScanResult?> = scanResultObserverSubject.asObservable()

    companion object {
        private var instance : Singleton? = null

        fun getInstance() : Singleton {

            if(instance == null) {
                instance = Singleton()
            }

            return instance!!
        }
    }


    fun updateScanResult(result: ScanResult?) {

        scanResultObserverSubject.onNext(result)

    }
}