# nortantis
Nortantis 是一个奇幻地图生成器，最初作为一个学术项目创建。它使用简单的构造板块模拟来生成带有树木、河流和山脉的岛屿和大陆。生成的地图具有老式手绘地图的外观。

有关更多信息和生成的地图示例，请参见项目页面 [这里](http://jeheydorn.github.io/nortantis/)。

本项目从 [https://github.com/jeheydorn/nortantis](https://github.com/jeheydorn/nortantis) 翻译而来（英文翻译成中文）。

# 安装
在(页面)[https://github.com/gjhhust/nortantis/releases]中下载最新版本的安装程序(.msi文件)。

# 开发

## 在vscode上运行和开发该项目
1. 在 vscode 中安装 gradle
2. 在 vscode 中打开项目
3. 在 vscode 中打开终端
4. 在 gradle 任务中运行任务 "run

## 打包安装程序
1. src\nortantis\util\AssetsPath.java 中将 public static boolean isInstalled = true;
2. 运行installer\create_windows_installer.bat即可打包成安装程序