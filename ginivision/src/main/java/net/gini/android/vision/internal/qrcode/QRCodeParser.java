package net.gini.android.vision.internal.qrcode;

import android.support.annotation.NonNull;

/**
 * Created by Alpar Szotyori on 08.12.2017.
 *
 * Copyright (c) 2017 Gini GmbH.
 */

/**
 * Interface for parsing QRCode content strings.
 *
 * @param <T> parsing output class
 */
interface QRCodeParser<T> {

    T parse(@NonNull final String qrCodeContent) throws IllegalArgumentException;
}
