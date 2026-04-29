package icons;

import com.intellij.openapi.util.IconLoader;

import javax.swing.*;

public interface ToolboxIcons {
  Icon Globe = IconLoader.getIcon("/images/icons/globe.svg", ToolboxIcons.class);
  Icon Atom = IconLoader.getIcon("/images/icons/atom.svg", ToolboxIcons.class);
  Icon Box = IconLoader.getIcon("/images/icons/box.svg", ToolboxIcons.class);
  Icon Chart = IconLoader.getIcon("/images/icons/chart.svg", ToolboxIcons.class);
  Icon Check = IconLoader.getIcon("/images/icons/check.svg", ToolboxIcons.class);
  Icon Close = IconLoader.getIcon("/images/icons/close.svg", ToolboxIcons.class);
  Icon Code = IconLoader.getIcon("/images/icons/code.svg", ToolboxIcons.class);
  Icon CodeFile = IconLoader.getIcon("/images/icons/code-file.svg", ToolboxIcons.class);
  Icon Puzzle = IconLoader.getIcon("/images/icons/puzzle.svg", ToolboxIcons.class);
  Icon Stack = IconLoader.getIcon("/images/icons/stack.svg", ToolboxIcons.class);
  Icon Suitcase = IconLoader.getIcon("/images/icons/suitcase.svg", ToolboxIcons.class);
  Icon VeevaOrange = IconLoader.getIcon("/images/icons/veeva-orange.svg", ToolboxIcons.class);
  Icon VeevaMulti = IconLoader.getIcon("/images/icons/veeva-multi.svg", ToolboxIcons.class);
  Icon VeevaGrey = IconLoader.getIcon("/images/icons/veeva-grey.svg", ToolboxIcons.class);
  Icon DatabaseDuotone = IconLoader.getIcon("/images/icons/database-duotone.svg", ToolboxIcons.class);
  Icon Bug = IconLoader.getIcon("/images/icons/bug.svg", ToolboxIcons.class);
  Icon Download = IconLoader.getIcon("/images/icons/download.svg", ToolboxIcons.class);
  Icon DoubleRight = IconLoader.getIcon("/images/icons/double-right.svg", ToolboxIcons.class);
  Icon Folder = IconLoader.getIcon("/images/icons/folder.svg", ToolboxIcons.class);
  Icon Gear = IconLoader.getIcon("/images/icons/gear.svg", ToolboxIcons.class);
  Icon Hammer = IconLoader.getIcon("/images/icons/hammer.svg", ToolboxIcons.class);
  Icon Files = IconLoader.getIcon("/images/icons/files.svg", ToolboxIcons.class);
  Icon Lock = IconLoader.getIcon("/images/icons/lock.svg", ToolboxIcons.class);
  Icon SlidersHorizontal = IconLoader.getIcon("/images/icons/sliders-horizontal.svg", ToolboxIcons.class);
  Icon Pencil = IconLoader.getIcon("/images/icons/pencil.svg", ToolboxIcons.class);
  Icon Pizza = IconLoader.getIcon("/images/icons/pizza.svg", ToolboxIcons.class);
  Icon Cpu = IconLoader.getIcon("/images/icons/cpu.svg", ToolboxIcons.class);
  Icon SignIn = IconLoader.getIcon("/images/icons/sign-in.svg", ToolboxIcons.class);
  Icon SignOut = IconLoader.getIcon("/images/icons/sign-out.svg", ToolboxIcons.class);
  Icon Terminal = IconLoader.getIcon("/images/icons/terminal.svg", ToolboxIcons.class);
  Icon Upload = IconLoader.getIcon("/images/icons/upload.svg", ToolboxIcons.class);
  Icon User = IconLoader.getIcon("/images/icons/user.svg", ToolboxIcons.class);
  Icon Package = IconLoader.getIcon("/images/icons/package.svg", ToolboxIcons.class);
  Icon FolderStar = IconLoader.getIcon("/images/icons/folder-star.svg", ToolboxIcons.class);
  Icon VeevaXml = IconLoader.getIcon("/images/icons/veeva-xml.svg", ToolboxIcons.class);

  // Aliases
  Icon Api = Globe;
  Icon Component = Puzzle;
  Icon ComponentFolder = Stack;
  Icon ConfigFolder = Suitcase;
  Icon Connected = VeevaOrange;
  Icon Configured = VeevaMulti;
  Icon Disconnected = VeevaGrey;
  Icon Database = DatabaseDuotone;
  Icon Debug = Bug;
  Icon Json = Atom;
  Icon Link = VeevaGrey;
  Icon Logs = Files;
  Icon LogsFolder = Files;
  Icon Menu = VeevaOrange;
  Icon Mdl = Puzzle;
  Icon Operations = SlidersHorizontal;
  Icon Runtime = Cpu;
  Icon Vpk = Package;
  Icon VpkFolder = FolderStar;
  Icon Xml = VeevaXml;
}
