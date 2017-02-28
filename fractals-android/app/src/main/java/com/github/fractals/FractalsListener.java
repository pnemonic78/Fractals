/*
 * Source file of the Remove Duplicates project.
 * Copyright (c) 2016. All Rights Reserved.
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/2.0
 *
 * Contributors can be contacted by electronic mail via the project Web pages:
 *
 * https://github.com/pnemonic78/Fractals
 *
 * Contributor(s):
 *   Moshe Waisberg
 *
 */
package com.github.fractals;

/**
 * Fractals event listener.
 *
 * @author Moshe Waisberg
 */
public interface FractalsListener {

    void onRenderFieldZoom(FractalsView view, double scale);

    void onRenderFieldPan(FractalsView view, int dx, int dy);

    void onRenderFieldStarted(FractalsView view);

    void onRenderFieldFinished(FractalsView view);

    void onRenderFieldCancelled(FractalsView view);
}
