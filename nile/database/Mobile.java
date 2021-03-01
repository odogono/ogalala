// $Id: Mobile.java,v 1.3 1998/05/02 14:48:31 jim Exp $
// Mobile class for Death on the Nile
// James Fryer, 27 March 98
// Copyright (C) Ogalala Ltd. <info@ogalala.com>

package com.ogalala.nile.database;

/** A Mobile is a Noun with intelligence
*/
class Mobile
    extends Noun
    {
    public Mobile()
        { }

    public Mobile(String id, String creator)
        {
        super(id, creator);
        }
    }