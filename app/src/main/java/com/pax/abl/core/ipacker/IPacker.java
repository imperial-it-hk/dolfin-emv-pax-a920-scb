/*
 * ============================================================================
 * COPYRIGHT
 *              Pax CORPORATION PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or
 *   nondisclosure agreement with Pax Corporation and may not be copied
 *   or disclosed except in accordance with the terms in that agreement.
 *      Copyright (C) 2016 - ? Pax Corporation. All rights reserved.
 * Module Date: 2016-11-25
 * Module Author: Steven.W
 * Description:
 *
 * ============================================================================
 */
package com.pax.abl.core.ipacker;


import androidx.annotation.NonNull;

/**
 * Packer interface
 *
 * @param <T>
 * @param <O>
 * @author Steven.W
 */
public interface IPacker<T, O> {
    /**
     * pack
     *
     * @param t input transaction data structure
     * @return output data
     */
    @NonNull
    O pack(@NonNull T t);

    /**
     * unpack
     *
     * @param t transaction data structure
     * @param o input data
     * @return result
     */
    int unpack(@NonNull T t, O o);
}
