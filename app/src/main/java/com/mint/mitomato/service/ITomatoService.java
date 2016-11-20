package com.mint.mitomato.service;

import rx.Observable;

/**
 * Created by mint924 on 2016/10/24.
 */

public interface ITomatoService {
    Observable<Integer> getStateObservable();

    Observable<Long> getRemainTimeObservable();
}
