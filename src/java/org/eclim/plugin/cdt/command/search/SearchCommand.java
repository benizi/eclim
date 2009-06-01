/**
 * Copyright (C) 2005 - 2009  Eric Van Dewoestine
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
package org.eclim.plugin.cdt.command.search;

import java.util.ArrayList;

import org.eclim.annotation.Command;

import org.eclim.command.CommandLine;
import org.eclim.command.Options;

import org.eclim.plugin.cdt.PluginResources;

import org.eclim.plugin.cdt.util.ASTUtils;
import org.eclim.plugin.cdt.util.CUtils;

import org.eclim.plugin.core.command.AbstractCommand;

import org.eclim.plugin.core.util.ProjectUtils;
import org.eclim.plugin.core.util.VimUtils;

import org.eclim.util.CollectionUtils;

import org.eclipse.cdt.core.CCorePlugin;

import org.eclipse.cdt.core.browser.ITypeReference;

import org.eclipse.cdt.core.dom.IName;

import org.eclipse.cdt.core.dom.ast.IASTFileLocation;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.IBinding;

import org.eclipse.cdt.core.index.IIndex;
import org.eclipse.cdt.core.index.IIndexManager;

import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.core.model.ITranslationUnit;

import org.eclipse.cdt.internal.ui.search.CSearchMessages;
import org.eclipse.cdt.internal.ui.search.PDOMSearchMatch;
import org.eclipse.cdt.internal.ui.search.PDOMSearchPatternQuery;
import org.eclipse.cdt.internal.ui.search.PDOMSearchQuery;
import org.eclipse.cdt.internal.ui.search.PDOMSearchResult;
import org.eclipse.cdt.internal.ui.search.TypeInfoSearchElement;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.search.ui.text.Match;

/**
 * Command to handle search requests.
 *
 * @author Eric Van Dewoestine
 */
@Command(
  name = "c_search",
  options =
    "REQUIRED n project ARG," +
    "OPTIONAL f file ARG," +
    "OPTIONAL o offset ARG," +
    "OPTIONAL l length ARG," +
    "OPTIONAL e encoding ARG," +
    "OPTIONAL p pattern ARG," +
    "OPTIONAL t type ARG," +
    "OPTIONAL x context ARG," +
    "OPTIONAL s scope ARG," +
    "OPTIONAL i case_insensitive NOARG"
)
public class SearchCommand
  extends AbstractCommand
{
  public static final String CONTEXT_ALL = "all";
  public static final String CONTEXT_DECLARATIONS = "declarations";
  public static final String CONTEXT_REFERENCES = "references";

  public static final String SCOPE_ALL = "all";
  public static final String SCOPE_PROJECT = "project";

  public static final String TYPE_ALL = "all";
  public static final String TYPE_CLASS_STRUCT = "class_struct";
  public static final String TYPE_FUNCTION = "function";
  public static final String TYPE_VARIABLE = "variable";
  public static final String TYPE_UNION = "union";
  public static final String TYPE_METHOD = "method";
  public static final String TYPE_FIELD = "field";
  public static final String TYPE_ENUM = "enum";
  public static final String TYPE_ENUMERATOR = "enumerator";
  public static final String TYPE_NAMESPACE = "namespace";
  public static final String TYPE_TYPEDEF = "typedef";
  public static final String TYPE_MACRO = "macro";

  /**
   * {@inheritDoc}
   * @see org.eclim.command.Command#execute(CommandLine)
   */
  public String execute(CommandLine commandLine)
    throws Exception
  {
    String projectName = commandLine.getValue(Options.NAME_OPTION);
    String file = commandLine.getValue(Options.FILE_OPTION);
    String offset = commandLine.getValue(Options.OFFSET_OPTION);
    String length = commandLine.getValue(Options.LENGTH_OPTION);

    IProject project = ProjectUtils.getProject(projectName);
    ICProject cproject = CUtils.getCProject(project);

    // element search
    if(file != null && offset != null && length != null){
      return executeElementSearch(commandLine, cproject);
    }

    // pattern search
    return executePatternSearch(commandLine, cproject);
  }

  private String executeElementSearch(
      CommandLine commandLine, ICProject cproject)
    throws Exception
  {
    StringBuffer buffer = new StringBuffer();

    String file = commandLine.getValue(Options.FILE_OPTION);
    ITranslationUnit src = CUtils.getTranslationUnit(cproject, file);
    if(src != null){
      int context = getContext(commandLine.getValue(Options.CONTEXT_OPTION));

      ICProject[] scope = new ICProject[]{cproject};
      if (SCOPE_ALL.equals(commandLine.getValue(Options.SCOPE_OPTION))){
        IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
        ArrayList<ICProject> cprojects = new ArrayList<ICProject>();
        for (IProject project : projects){
          if (project.isOpen() && (
              project.hasNature(PluginResources.NATURE_C) ||
              project.hasNature(PluginResources.NATURE_CPP)))
          {
            cprojects.add(CUtils.getCProject(project));
          }
        }
        scope = cprojects.toArray(new ICProject[cprojects.size()]);
      }

      IIndex index = CCorePlugin.getIndexManager().getIndex(
          scope, IIndexManager.ADD_DEPENDENCIES | IIndexManager.ADD_DEPENDENT);
      index.acquireReadLock();
      try{
        IASTTranslationUnit ast = ASTUtils.getTranslationUnit(src);
        IASTName node = ast.getNodeSelector(null)
          .findEnclosingName(
              getOffset(commandLine),
              commandLine.getIntValue(Options.LENGTH_OPTION));
        IBinding binding = node.resolveBinding();

        ArrayList<IName> names = new ArrayList<IName>();

        if (context == PDOMSearchQuery.FIND_ALL_OCCURANCES ||
            context == PDOMSearchQuery.FIND_DECLARATIONS_DEFINITIONS)
        {
          CollectionUtils.addAll(names, ast.getDefinitions(binding));
          IName[] decs = ast.getDeclarations(binding);
          for (IName n : decs){
            if (!names.contains(n)){
              names.add(n);
            }
          }
        }

        if (context == PDOMSearchQuery.FIND_ALL_OCCURANCES ||
            context == PDOMSearchQuery.FIND_REFERENCES)
        {
          CollectionUtils.addAll(names, ast.getReferences(binding));
        }

        for (IName name : names){
          if(buffer.length() > 0){
            buffer.append('\n');
          }
          IASTFileLocation loc = name.getFileLocation();
          String filename = loc.getFileName();
          String lineColumn =
            VimUtils.translateLineColumn(filename, loc.getNodeOffset());
          buffer.append(filename)
            .append('|')
            .append(lineColumn)
            .append('|')
            .append("");
        }
      }finally{
        index.releaseReadLock();
      }
    }

    return buffer.toString();
  }

  private String executePatternSearch(
      CommandLine commandLine, ICProject cproject)
    throws Exception
  {
    ICProject[] scope = getScope(
        commandLine.getValue(Options.SCOPE_OPTION), cproject);
    String scopeDesc = null;
    if (SCOPE_ALL.equals(scope)){
      scopeDesc = CSearchMessages.WorkspaceScope;
    }else if (SCOPE_PROJECT.equals(scope)){
      scopeDesc = CSearchMessages.ProjectScope;
    }

    int context = getContext(commandLine.getValue(Options.TYPE_OPTION));
    int type = getType(commandLine.getValue(Options.TYPE_OPTION));
    String pattern = commandLine.getValue(Options.PATTERN_OPTION);
    boolean caseSensitive =
      !commandLine.hasOption(Options.CASE_INSENSITIVE_OPTION);
    PDOMSearchQuery query = new PDOMSearchPatternQuery(
        scope, scopeDesc, pattern, caseSensitive, type | context);

    StringBuffer buffer = new StringBuffer();
    if (query != null){
      query.run(new NullProgressMonitor());
      PDOMSearchResult result = (PDOMSearchResult)query.getSearchResult();
      for (Object e : result.getElements()){
        for (Match m : result.getMatches(e)){
          PDOMSearchMatch match = (PDOMSearchMatch)m;
          TypeInfoSearchElement element =
            (TypeInfoSearchElement)match.getElement();
          ITypeReference ref = element.getTypeInfo().getResolvedReference();
          if(ref != null){
            if(buffer.length() > 0){
              buffer.append('\n');
            }
            String filename = ref.getLocation().toOSString();
            String lineColumn =
              VimUtils.translateLineColumn(filename, ref.getOffset());
            buffer.append(filename)
              .append('|')
              .append(lineColumn)
              .append('|')
              .append("");
          }
        }
      }
    }

    return buffer.toString();
  }

  /**
   * Gets the search scope to use.
   *
   * @param scope The string name of the scope.
   * @param project The current project.
   *
   * @return The ICProject array representing the scope.
   */
  protected ICProject[] getScope(String scope, ICProject project)
    throws Exception
  {
    if (SCOPE_ALL.equals(scope)){
      return null;
    }

    ArrayList<ICProject> elements = new ArrayList<ICProject>();
    elements.add(project);
    IProject[] depends = project.getProject().getReferencedProjects();
    for (IProject p : depends){
      if(!p.isOpen()){
        p.open(null);
      }
      elements.add(CoreModel.getDefault().create(p));
    }
    return elements.toArray(new ICProject[elements.size()]);
  }

  /**
   * Translates the string context to the int equivalent.
   *
   * @param context The String context.
   * @return The int context
   */
  protected int getContext(String context)
  {
    if(CONTEXT_ALL.equals(context)){
      return PDOMSearchQuery.FIND_ALL_OCCURANCES;
    }else if(CONTEXT_REFERENCES.equals(context)){
      return PDOMSearchQuery.FIND_REFERENCES;
    }
    return PDOMSearchQuery.FIND_DECLARATIONS_DEFINITIONS;
  }

  /**
   * Translates the string type to the int equivalent.
   *
   * @param type The String type.
   * @return The int type.
   */
  protected int getType(String type)
  {
    if(TYPE_CLASS_STRUCT.equals(type)){
      return PDOMSearchPatternQuery.FIND_CLASS_STRUCT;
    }else if(TYPE_FUNCTION.equals(type)){
      return PDOMSearchPatternQuery.FIND_FUNCTION;
    }else if(TYPE_VARIABLE.equals(type)){
      return PDOMSearchPatternQuery.FIND_VARIABLE;
    }else if(TYPE_UNION.equals(type)){
      return PDOMSearchPatternQuery.FIND_UNION;
    }else if(TYPE_METHOD.equals(type)){
      return PDOMSearchPatternQuery.FIND_METHOD;
    }else if(TYPE_FIELD.equals(type)){
      return PDOMSearchPatternQuery.FIND_FIELD;
    }else if(TYPE_ENUM.equals(type)){
      return PDOMSearchPatternQuery.FIND_ENUM;
    }else if(TYPE_ENUMERATOR.equals(type)){
      return PDOMSearchPatternQuery.FIND_ENUMERATOR;
    }else if(TYPE_NAMESPACE.equals(type)){
      return PDOMSearchPatternQuery.FIND_NAMESPACE;
    }else if(TYPE_TYPEDEF.equals(type)){
      return PDOMSearchPatternQuery.FIND_TYPEDEF;
    }else if(TYPE_MACRO.equals(type)){
      return PDOMSearchPatternQuery.FIND_MACRO;
    }
    return PDOMSearchPatternQuery.FIND_ALL_TYPES;
  }
}
