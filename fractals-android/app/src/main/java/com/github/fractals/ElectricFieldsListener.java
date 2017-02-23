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
 * https://github.com/pnemonic78/Electric-Fields
 *
 * Contributor(s):
 *   Moshe Waisberg
 *
 */
package com.github.fractals;

/**
 * Electric fields event listener.
 *
 * @author Moshe Waisberg
 */
public interface ElectricFieldsListener {

    void onChargeAdded(ElectricFieldsView view, Charge charge);

    void onChargeInverted(ElectricFieldsView view, Charge charge);

    void onRenderFieldStarted(ElectricFieldsView view);

    void onRenderFieldFinished(ElectricFieldsView view);

    void onRenderFieldCancelled(ElectricFieldsView view);
}
