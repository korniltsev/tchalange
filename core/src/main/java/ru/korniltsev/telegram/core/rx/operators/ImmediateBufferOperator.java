package ru.korniltsev.telegram.core.rx.operators;

import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;
import rx.functions.Action0;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;


public class ImmediateBufferOperator<T> implements Observable.Operator<List<T>, T> {
    final Scheduler scheduler;
    final long interval;


    public ImmediateBufferOperator(Scheduler scheduler, long interval) {
        this.scheduler = scheduler;
        this.interval = interval;
    }

    @Override
    public Subscriber<? super T> call(final Subscriber<? super List<T>> subscriber) {


        return new Subscriber<T>() {
            final Scheduler.Worker worker = scheduler.createWorker();

            {
                subscriber.add(worker);
            }

            //guarded by lock
            final List<T> buffer = new ArrayList<>();
            //guarded by lock
            long lastEmmit = 0;
            //guarded by lock
            boolean scheduled = false;
            final Object lock = new Object();

            @Override
            public void onCompleted() {
                subscriber.onCompleted();
            }

            @Override
            public void onError(Throwable e) {
                subscriber.onError(e);
            }

            @Override
            public void onNext(T t) {
                synchronized (lock) {
                    buffer.add(t);
                    if (scheduled){
                        return;
                    }
                    long timeAfterLastEmmitPlusBuffer = now() - lastEmmit - interval;
                    if (lastEmmit == 0 || timeAfterLastEmmitPlusBuffer >= 0) {
                        emmitBuffer(subscriber);
                    } else {
                        scheduled = true;
                        worker.schedule(new Action0() {
                            @Override
                            public void call() {
                                synchronized (lock) {
                                    emmitBuffer(subscriber);
                                    scheduled = false;
                                }
                            }
                        }, interval, TimeUnit.MILLISECONDS);
                    }
                }

            }

            private void emmitBuffer(Subscriber<? super List<T>> s) {
                s.onNext(new ArrayList<>(buffer));
                buffer.clear();
                lastEmmit = now();
            }

        };
    }

    private long now() {
        return scheduler.now();
    }


}
