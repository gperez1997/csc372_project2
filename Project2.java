
/*
 * Germán Pérez Arispuro and Brock Gordon
 * CSC 372 Spring 2022
 * Project 2
 * 
 * For Project 2, Brock and I decided to do something simple but
 * still functional. The language, while parsed with Java, is translated
 * into Python. This choice was for simplicity of parsing and the use of
 * simpler Regular Expressions and, a small try at, pattern matching.
 * Programs can be read into the translator with a file or with a
 * command line argument. However, you should avoid writing lines in the
 * command line argument format since the translator will catch those
 * as syntactic errors. We thought the writing a command on file won't make
 * a lot of sense; although, we did plan for adding it and parse such lines.
 * 
 * Most of the parsing is done with Regular Expressions (regex), however,
 * this made it that a lot of lines were needed for creating the pattern and
 * matching it. We tried to divide things up as best we could to avoid having
 * a ton of code in one single function (some still have a lot of lines).
 * Pattern matching by conditionals was an idea at the start but the complexity
 * of such approach meant we would need a ton more time to implement (not
 * counting how long and confusing it could get).
 * 
 * The slides go over the syntax but if something is not clear please ask!
 * There might have been some things we missed in our descriptions.
 * 
 * To read a command into a file with the translated program you do:
 * 		$ java -jar project2.jar do "out("Hello")" > translated.py
 * 		$ python translated.py
 * 		$ java -jar project2.jar do "notFalse @ True" > translated.py
 * 		$ python translated.py
 * 
 * To read a program file, you would do:
 * 		$ java -jar project2.jar filename.txt > translated.py
 * 		$ python translated.py
 * 
 * Running these on -jar is because my computer (Germán's) for some
 * reason either didn't identify java or javac, or didn't find the
 * compiler file, even when moved around to the working directory.
 * If you can run it using the 'java' command by itself, awesome!
 * You would just need the Project2.java files to run (this one here).
 * 
 * Be careful! Like we mentioned, the language is supposed to be simple
 * but still functional; however, there is no way for some lines to be
 * processed correctly if they contain multiple operations. The only way
 * these work is by creating a printing line but that won't store big
 * operations to a variable. You can do things like:
 * 	int more = 5 + 10$
 * But not (at least, not outside a printing statement):
 *  int more 5 + 10 + 20$
 *  
 * Another point is when using conditions for if-statements or loops;
 * for these, avoid using <= or >=, this is because the way the pattern
 * is checked to make sure that a single '=' is changed to '==' for Python
 * to run will change the '=' in '<=' or '>='. If you want to do a
 * comparison like these, you can initialize a boolean as with such and
 * operation and use that as your condition.
 * 
 * Indentation to keep blocks of code aligned was taken care of by using
 * a global variable, that way, we don't need to call it in every single
 * function, which will make sure that indentation is kept for any line.
 * 
 * Hopefully, the slides and test programs can help guide you in what you
 * can do with this language and how to use it. Again, if something is not
 * clear, let us know! We were prone to miss details while working on this,
 * so missing a description somewhere might happen (will triple check everything
 * to make sure that's not the case).
 * 
 */
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Project2 {

	private static int tabIndents = 0;

	public static void main(String[] args) {
		if (args.length == 0) {
			System.out.printf("No arguments passed!%n");
		}
		if (args[0].equals("do")) {
			checkCommand(args[1]);
		} else {
			System.out.println("import sys");
			readFile(args[0]);
		}
	}

	// Checks for lines will need to be in individual functions
	// so that the same thing is done from command line and a file.
	public static void checkLine(String line) {
		line = line.trim();
		Pattern comment = Pattern.compile("^~(.*)~$");
		Matcher commLine = comment.matcher(line);

		Pattern varChange = Pattern.compile("^\\w+\s[=]\s.*\\$$");
		Matcher varToVar = varChange.matcher(line);
		Pattern whilePat = Pattern.compile("^while\s\\{(\\w+|\\d+)\s?[<>=]?\s?.*\\}:$");
		Matcher whileLoop = whilePat.matcher(line);
		Pattern ifState = Pattern.compile(
						"^(if)\s(not)?\\{\\-*(not)?.*\s?[<>=@\\^]?\s?\\-*(not)?\\w*\\}\s(then):$");
		Matcher ifLine = ifState.matcher(line);
		Pattern closeLoopIf = Pattern.compile("^;$");
		Matcher closer = closeLoopIf.matcher(line);

		Pattern increase = Pattern.compile("^\\w+\s((\\+=)|(\\-=)|(\\*=))\s.*\\$$");
		Matcher varIncr = increase.matcher(line);

		String[] lineParts = line.split(" ");
		if (lineParts[0].equals("int") || lineParts[0].equals("word") || lineParts[0].equals(
						"bool")) {
			assignationLine(line);
		} else if (varToVar.find()) {
			changeVariable(varToVar.group());
		} else if (ifLine.find()) {
			processIfStatement(ifLine.group());
			tabIndents += 1;
		} else if (whileLoop.find()) {
			processWhileLoop(whileLoop.group());
			tabIndents += 1;
		} else if (closer.find()) {
			tabIndents -= 1;
			System.out.println();
		} else if (line.startsWith("out(\")")) {
			Pattern regMessage = Pattern.compile("^out\\(\".*\".*\\)\\$$");
			Matcher message = regMessage.matcher(line);
			if (message.find()) {
				printIndentations();
				System.out.printf("print(\"%s\")%n", line.substring(4, line.length() - 2));
			} else {
				System.out.println("print(\"Missing ')', end quotes, or end of line ('$')\")");
			}
		} else if (line.startsWith("out(")) {
			Pattern regMessage = Pattern.compile("^out\\(.*\\)\\$$");
			Matcher message = regMessage.matcher(line);
			if (message.find()) {
				line = checkBoolMessage(line);
				printIndentations();
				System.out.printf("print(%s)%n", line.substring(4, line.length() - 2));
			} else {
				System.out.println("print(\"Missing ')', or end of line ('$')\")");
			}
		} else if (varIncr.find()) {
			increaseCounterVar(varIncr.group());
		} else if (commLine.find()) {
			addComment(commLine.group());
		} else if (line.equals("") || line.equals("\n")) {
			System.out.println();
		} else {
			System.out.println("print(\"Invalid syntax.\")");
			System.out.printf("print(\"Line '%s' is not valid.\")%n", line);
			System.exit(0);
		}
	}

	public static void assignationLine(String line) {
		Pattern intValue = Pattern.compile("^int\s[^0-9 ]+\s[=]\s\\-*[0-9]+\\$$");
		Matcher intLine = intValue.matcher(line);
		Pattern wordValue = Pattern.compile("^word\s[^0-9 ]+\s[=]\s\".*\"\\$$");
		Matcher wordLine = wordValue.matcher(line);
		Pattern boolValue = Pattern.compile("^bool\s[^0-9 ]+\s[=]\s(not)?(True|False)\\$$");
		Matcher boolLine = boolValue.matcher(line);

		Pattern intVariable = Pattern.compile("^int\s[^0-9 ]+\s[=]\s\\-*.*\\$$");
		Matcher intVar = intVariable.matcher(line);
		Pattern wordVariable = Pattern.compile("^word\s[^0-9 ]+\s[=]\s.*\\$$");
		Matcher wordVar = wordVariable.matcher(line);
		Pattern boolVariable = Pattern.compile("^bool\s[^0-9 ]+\s[=]\s(not)?.*\\$$");
		Matcher boolVar = boolVariable.matcher(line);

		Pattern intExpression = Pattern.compile(
						"^int\s[^0-9 ]+\s[=]\s\\-*\\w+\s[+\\-/*%]\s\\-*\\w+\\$$");
		Matcher intExpr = intExpression.matcher(line);
		Pattern wordExpression = Pattern.compile(
						"^word\s\\w+\s[=]\s(\".*\"|.*)\s[+]\s(\".*\"|.*)\\$$");
		Matcher wordExpr = wordExpression.matcher(line);
		Pattern boolIntExpr = Pattern.compile(
						"^bool\s[^0-9 ]+\s[=]\s\\-*\\w+\s[<>=]+\s\\-*\\w+\\$$");
		Matcher boolInt = boolIntExpr.matcher(line);

		if (intLine.find()) {
			processValueAssign(intLine.group());
		} else if (wordLine.find()) {
			processValueAssign(wordLine.group());
		} else if (boolLine.find()) {
			processValueAssign(boolLine.group());
		} else if (intVar.find()) {
			processVariableAssign(intVar.group());
		} else if (wordVar.find()) {
			processVariableAssign(wordVar.group());
		} else if (boolVar.find()) {
			processVariableAssign(boolVar.group());
		} else if (intExpr.find()) {
			integerBoolExprAssign(intExpr.group());
		} else if (wordExpr.find()) {
			stringExprAssign(wordExpr.group());
		} else if (boolInt.find()) {
			integerBoolExprAssign(boolInt.group());
		} else {
			System.out.printf("print(\"Error on line: %s\")%n", line);
			System.out.println("print(\"Declaration not valid.\")");
			System.out.println("print(\"Missing '=' or value to assign.\")");
			System.out.println("print(\"Name might be missing from declaration.\")");
			System.exit(0);
		}
	}

	public static void checkCommand(String line) {
		Pattern doBoolean = Pattern.compile("^(not)?(True|False)\s[@\\^]\s(not)?(True|False)\\$$");
		Matcher boolCmd = doBoolean.matcher(line);
		Pattern doInts = Pattern.compile("^\\-*\\d+\s[+\\-/*%=<>]\s\\-*\\d+\\$$");
		Matcher intCmd = doInts.matcher(line);
		Pattern doPrintStr = Pattern.compile("^out\\(\".*\"\\)\\$$");
		Matcher printStrCmd = doPrintStr.matcher(line);
		Pattern doPrintExpr = Pattern.compile("^out\\(.*\\)\\$$");
		Matcher printExprCmd = doPrintExpr.matcher(line);

		if (boolCmd.find()) {
			boolOperation(boolCmd.group());
		} else if (intCmd.find()) {
			integerOperation(intCmd.group());
		} else if (printStrCmd.find()) {
			printRegMessage(printStrCmd.group());
		} else if (printExprCmd.find()) {
			printExpression(printExprCmd.group());
		} else {
			String format = line.replaceAll("\n", "");
			System.out.printf("command = \"%s\"%n", format);
			System.out.println("print(\"Syntax error in line: \", command)");
		}
	}

	public static void readFile(String filename) {
		Scanner codeFile = null;

		try {
			codeFile = new Scanner(new BufferedReader(new FileReader(filename)));
			System.out.println("print(\"File found.\")");
		} catch (FileNotFoundException e) {
			System.out.println("print(\"Command not understood.\")");
			System.out.println("print(\"If reading a file, make sure file path is correct.\")");
			System.out.println("print(\"If running commands, make sure to use: do <command>\")");
		}

		while (codeFile.hasNext()) {
			String codeLine = codeFile.nextLine().trim();
			checkLine(codeLine);
		}
	}

	public static void boolOperation(String line) {
		String[] parts = line.split(" ");
		String value1 = parts[0];
		String boolOp = boolOperator(parts[1]);
		String value2 = parts[2].replaceAll("\\$", "");
		if (value1.substring(0, 3).equals("not")) {
			value1 = "not " + value1.substring(3);
		}
		if (value2.substring(0, 3).equals("not")) {
			value2 = "not " + value2.substring(3);
		}
		printIndentations();
		System.out.printf("print(%s %s %s)%n", value1, boolOp, value2);
	}

	public static String boolOperator(String operator) {
		if (operator.equals("@")) {
			return "and";
		} else {
			return "or";
		}
	}

	public static String checkBoolMessage(String line) {
		if (line.contains("=")) {
			line = line.replaceAll("=", "==");
		}
		if (line.contains("not")) {
			line = line.replaceAll("not", "not ");
		}
		if (line.contains("@")) {
			line = line.replaceAll("@", "and");
		}
		if (line.contains("^")) {
			line = line.replaceAll("\\^", "or");
		}
		return line;
	}

	public static void integerOperation(String line) {
		String[] parts = line.split(" ");
		String value1 = parts[0];
		String intOp = parts[1];
		String value2 = parts[2].replaceAll("\\$", "");
		if (intOp.equals("/")) {
			intOp = "//";
		}
		if (intOp.equals("=")) {
			intOp = "==";
		}
		printIndentations();
		System.out.printf("print(%s %s %s)%n", value1, intOp, value2);
	}

	public static void printRegMessage(String line) {
		String message = line.substring(4, line.length() - 2);
		printIndentations();
		System.out.printf("print(%s)", message);
	}

	public static void printExpression(String line) {
		String printed = line.substring(4, line.length() - 2);
		if (printed.contains("=")) {
			int indexOfEqual = printed.indexOf("=");
			String start = printed.substring(0, indexOfEqual);
			String end = printed.substring(indexOfEqual + 1, printed.length());
			printed = start + "==" + end;
		}
		printIndentations();
		System.out.printf("print(%s)%n", printed);
	}

	public static void processValueAssign(String line) {
		line = line.substring(0, line.length() - 1);
		String[] valueAssign = line.split("=");
		String varName = valueAssign[0].split(" ")[1].trim();
		String value = valueAssign[1].trim();
		if (value.length() > 2) {
			if (value.substring(0, 3).equals("not")) {
				value = "not " + value.substring(3);
			}
		}
		invalidName(varName);
		printIndentations();
		System.out.printf("%s = %s%n", varName, value);
	}

	public static void processVariableAssign(String line) {
		line = line.substring(0, line.length() - 1);
		if (line.contains("args")) {
			line = line.replaceAll("cmd.args", "sys.argv");
		}
		String[] variableAssign = line.split("=");
		String varName = variableAssign[0].split(" ")[1].trim();
		String variableToAssign = variableAssign[1].trim();
		if (variableToAssign.length() > 2) {
			if (variableToAssign.substring(0, 3).equals("not")) {
				variableToAssign = "not " + variableToAssign.substring(3);
			}
		}
		invalidName(varName);
		printIndentations();
		System.out.printf("%s = %s%n", varName, variableToAssign);
	}

	public static void integerBoolExprAssign(String line) {
		line = line.substring(0, line.length() - 1);
		String[] exprAssign = line.split("=", 2);
		String varName = exprAssign[0].split(" ")[1].trim();
		invalidName(varName);
		String[] expression = exprAssign[1].trim().split(" ");
		String expr = null;
		if (expression[1].equals("/")) {
			expression[1] = "//";
		}
		if (expression[1].equals("=")) {
			expression[1] = "==";
		}
		expr = expression[0] + " " + expression[1] + " " + expression[2];
		printIndentations();
		System.out.printf("%s = %s%n", varName, expr);
	}

	public static void stringExprAssign(String line) {
		line = line.substring(0, line.length() - 1);
		String[] exprAssign = line.split("=");
		String varName = exprAssign[0].split(" ")[1].trim();
		String expr = exprAssign[1].trim();
		invalidName(varName);
		printIndentations();
		System.out.printf("%s = %s%n", varName, expr);
	}

	public static void changeVariable(String line) {
		line = line.substring(0, line.length() - 1);
		String[] variables = line.split("=");
		String variable1 = variables[0].trim();
		String variable2 = variables[1].trim();
		if (variable2.length() > 2) {
			if (variable2.substring(0, 3).equals("not")) {
				variable2 = "not " + variable2;
			}
		}
		printIndentations();
		System.out.printf("%s = %s%n", variable1, variable2);

	}

	public static void processIfStatement(String line) {
		line = line.replaceAll("\\{", "(");
		line = line.replaceAll("\\}", ")");
		// If using '<=' declare so in a bool variable and use that
		if (line.contains("=")) {
			line = line.replaceAll("=", "==");
		}
		if (line.contains("/")) {
			line = line.replaceAll("/", "//");
		}
		if (line.contains("@")) {
			line = line.replaceAll("@", "and");
		}
		if (line.contains("#")) {
			line = line.replaceAll("#", "or");
		}
		// Check if condition starts with negative
		if (line.substring(0, 6).contains("not")) {
			line = line.substring(0, 3) + "not " + line.substring(6, line.length() - 6);
		} else {
			line = line.substring(0, line.length() - 6);
		}
		printIndentations();
		System.out.printf("%s:%n", line);
	}

	public static void processWhileLoop(String line) {
		line = line.replaceAll("\\{", "(");
		line = line.replaceAll("\\}", ")");
		// If using '<=' declare so in a bool variable and use that
		if (line.contains("=")) {
			line = line.replaceAll("=", "==");
		}
		if (line.contains("/")) {
			line = line.replaceAll("/", "//");
		}
		line = line.substring(0, line.length() - 1);

		printIndentations();
		System.out.printf("%s:%n", line);
	}

	public static void addComment(String line) {
		line = "# " + line.substring(1, line.length() - 1);
		System.out.println(line);
	}

	public static void increaseCounterVar(String line) {
		line = line.substring(0, line.length() - 1);
		String[] expression = line.split(" ");
		String variable = expression[0];
		String increase = expression[2];
		String step = expression[1];
		printIndentations();
		System.out.printf("%s %s %s%n", variable, step, increase);

	}

	public static void printIndentations() {
		for (int i = 0; i < tabIndents; i++) {
			System.out.print("\t");
		}
	}

	public static void invalidName(String name) {
		if (name.toLowerCase().equals("true") || name.toLowerCase().equals("false")) {
			System.out.println("print(\"Variable name cannot be a boolean value.\")");
			System.exit(0);
		}
		if (name.equals("int") || name.equals("word") || name.equals("bool")) {
			System.out.println("print(\"Variable name cannot be a type name.\")");
			System.exit(0);
		}
		if (name.equals("if") || name.equals("while")) {
			System.out.println("print(\"Variable name cannot be 'if' or 'while'.\")");
			System.exit(0);
		}
		if (name.equals("not")) {
			System.out.println("print(\"Variable name cannot be negation operator.\")");
			System.exit(0);
		}

	}
}