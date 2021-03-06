.. Copyright (C) 2005 - 2009  Eric Van Dewoestine

   This program is free software: you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation, either version 3 of the License, or
   (at your option) any later version.

   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with this program.  If not, see <http://www.gnu.org/licenses/>.

.. _vim/java/format:

Java Source Code Formatting
===========================

Eclim provides the ability to format java source code using the eclipse
formatter selected for your workspace.

Source code formatting is invoked in eclipse using the shortcut <C-S-F>, or
from the ``Source / Format menu``.  The eclim equivalent is invoked using the
**:JavaFormat** command described below.


.. _\:JavaFormat:

- **:JavaFormat** -
  Formats the current visual selection (or the current line, if nothing is
  selected). To format the whole file, use :%JavaFormat.

Given the following file\:

.. code-block:: java

 /**
  * @return
  *
  * Service
  * for test Eclipse <C-F> formatting.
  */
  public
  static String
  getAbstractService
  ()
  {
    if (abstractService == null)
    {
      throw new RuntimeException( "abstractService isn't initialized !");
    }
    return abstractService;
  }

You can execute **:%JavaFormat** to format the code according to your eclipse
settings.

.. code-block:: java

  /**
    * @return
    *
    * Service for test Eclipse <C-F> formatting.
    */
  public static String getAbstractService() {
    if (abstractService == null) {
      throw new RuntimeException("abstractService isn't initialized !");
    }
    return abstractService;
  }

.. note::

  The formatting of the code is done externally with Eclipse and with
  that comes a couple of :ref:`caveats <vim/issues>`.


Configuration
-------------

Currently source code formatting is only configurable via the eclipse GUI.  To
do so, shutdown eclim, start the eclipse GUI and configure your settings via\:

::

  Preferences : Java / Code Style / Formatter / Active Profile: / Edit
