/**
 * Copyright (C) 2005 - 2008  Eric Van Dewoestine
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
import org.eclim.plugin.core.command.taglist.RegexTaglist;
import org.eclim.plugin.core.command.taglist.TaglistScript;
import org.eclim.plugin.core.command.taglist.TagResult;

/**
 * Processes tags for php files.
 */
class PhpTags implements TaglistScript
{
  public TagResult[] execute (String file)
  {
    def regex = null;
    try{
      regex = new RegexTaglist(file);
      regex.addPattern('f', ~/(s?)\bfunction\s+([a-zA-Z0-9_]+)\s*\(/, "\$2");
      regex.addPattern('c', ~/(s?)\bclass\s+([a-zA-Z0-9_]+)/, "\$2");
      regex.addPattern('i', ~/(s?)\binterface\s+([a-zA-Z0-9_]+)/, "\$2");

      return regex.execute();
    }finally{
      if (regex != null) regex.close();
    }
  }
}
