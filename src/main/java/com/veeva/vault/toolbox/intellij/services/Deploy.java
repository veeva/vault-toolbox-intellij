package com.veeva.vault.toolbox.intellij.services;

import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiManager;
import com.veeva.vault.toolbox.intellij.project.ToolboxProject;
import com.veeva.vault.toolbox.intellij.ui.Message;
import com.veeva.vault.vapil.api.model.response.VaultResponse;
import com.veeva.vault.vapil.api.request.MetaDataRequest;
import com.veeva.vault.vapil.api.request.SDKRequest;
import com.veeva.vault.vapil.api.model.response.MdlExecuteResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;

import static com.veeva.vault.toolbox.core.utils.Checksum.getMd5;

public class Deploy {
	private static final Logger logger = LoggerFactory.getLogger(Deploy.class);
	ToolboxProject toolboxProject;

	public Deploy(ToolboxProject toolboxProject) {
		this.toolboxProject = toolboxProject;
	}

	public void showResults(VaultResponse response, Message message) {
		try {
			int numErrors = 0;
			int numWarnings = 0;
			try {
				//if we have a response, show it
				if (response == null) {
					message.append("response = null");
				}
				else {
                    message.append(response.getResponseStatus(), true);
                    if (response.getResponseMessage() != null) {
                        message.appendSeparator();
                        message.append(response.getResponseMessage(), true);
                    }

					/*
					if (response.hasErrors()) {
						message.appendSeparator();
						for (VaultResponse.APIResponseWarning warning : response.getWarnings()) {
							message.append(warning.getMessage(), true);
							numWarnings++;
						}
					}

					 */

                    if (response.hasErrors()) {
                        message.appendSeparator();
                        for (VaultResponse.APIResponseError error : response.getErrors()) {
                            message.append(error.getMessage(), true);
                            numErrors++;
                        }
                    }

                    if (response instanceof MdlExecuteResponse mdlResponse) {
                        MdlExecuteResponse.ScriptExecution scriptExecution = mdlResponse.getScriptExecution();

                        if (scriptExecution != null) {
                            numWarnings = scriptExecution.getWarnings() != null ? scriptExecution.getWarnings() : 0;
                            numErrors = numErrors + (scriptExecution.getFailures() != null ? scriptExecution.getFailures() : 0)
                                    + (scriptExecution.getExceptions() != null ? scriptExecution.getExceptions() : 0);

                            message.appendSeparator();
                            message.append("message = " + scriptExecution.getMessage(), true);
                            message.append("warnings = " + scriptExecution.getWarnings(), true);
                            message.append("failures = " + scriptExecution.getFailures(), true);
                            message.append("exceptions = " + scriptExecution.getExceptions(), true);
                            message.append("components_affected = " + scriptExecution.getComponentsAffected(), true);
                        }

                        List<MdlExecuteResponse.StatementExecution> statementExecutions = mdlResponse.getStatementExecution();
                        if (statementExecutions != null) {
                            for (MdlExecuteResponse.StatementExecution script : statementExecutions) {
                                message.appendSeparator();
                                message.append("command = " + script.getCommand(), true);
                                message.append("component = " + script.getComponent(), true);
                                message.append("message = " + script.getMessage(), true);
                                message.append("response = " + script.getResponse(), true);

                                Object rawErrors = script.get("errors");
                                if (rawErrors instanceof java.util.List) {
                                    message.appendSeparator();
                                    for (Object errorObj : (java.util.List<?>) rawErrors) {
                                        if (errorObj instanceof java.util.Map) {
                                            message.append(String.valueOf(((java.util.Map<?, ?>) errorObj).get("message")), true);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
			}
			catch (Exception e) {
				numErrors = 1;
				message.append("exception = " + e.getMessage());
			}

			if (numErrors > 0) {
				message.showError();
			}
			else if (numWarnings > 0) {
				message.showWarning();
			}
			else {
				message.showInformation();
			}
		}
		catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	void deploy() {
		Project project = null;
		PsiFile psiFile = null;
		ToolboxProject toolboxProject = null;


		Message message = toolboxProject.newMessage();
		message.setTitle("Deploy to Vault: " + psiFile.getName());

		int numErrors = 0;
		int numWarnings = 0;
		try {
			FileDocumentManager.getInstance().saveDocument(psiFile.getViewProvider().getDocument());
			String fileContent = psiFile.getText();

			//get either the MDL or SDK response
			VaultResponse response = null;
			if (psiFile.getName().toLowerCase().endsWith(".mdl")) {
				response = toolboxProject.getVaultClient().newRequest(MetaDataRequest.class)
						.setRequestString(fileContent)
						.executeMDLScript();
			} else if (psiFile.getName().toLowerCase().endsWith(".java")) {
				response = toolboxProject.getVaultClient().newRequest(SDKRequest.class)
						.setBinaryFile(psiFile.getName(), fileContent.getBytes(StandardCharsets.UTF_8))
						.addOrReplaceSingleSourceCodeFile();
			}

			//if we have a response, show it
			if (response == null) {
				message.append("response = null");
			} else {

				if (!response.isFailure()) {
					String md5 = getMd5(fileContent);
					toolboxProject.includeFile(psiFile.getVirtualFile().getCanonicalPath(), md5);
				}

				message.append(response.getResponseStatus(), true);
				if (response.getResponseMessage() != null) {
					message.appendSeparator();
					message.append(response.getResponseMessage(), true);
				}

				if (response.hasErrors()) {
					message.appendSeparator();
					for (VaultResponse.APIResponseError error : response.getErrors()) {
						message.append(error.getMessage(), true);
						numErrors++;
					}
				}

				LinkedHashMap<String, Object> scriptExecution = (LinkedHashMap<String, Object>) response.get("script_execution");
				if (scriptExecution != null) {
					numWarnings = (int) scriptExecution.get("warnings");
					numErrors = numErrors + (int) scriptExecution.get("failures") + (int) scriptExecution.get("exceptions");

					message.appendSeparator();
					message.append("message = " + scriptExecution.get("message"), true);
					message.append("warnings = " + scriptExecution.get("warnings"), true);
					message.append("failures = " + scriptExecution.get("failures"), true);
					message.append("exceptions = " + scriptExecution.get("exceptions"), true);
					message.append("components_affected = " + scriptExecution.get("components_affected"), true);
				}

				List<LinkedHashMap<String, Object>> statementExecutions = (List<LinkedHashMap<String, Object>>) response.get("statement_execution");
				if (statementExecutions != null) {
					for (LinkedHashMap<String, Object> script : statementExecutions) {
						message.appendSeparator();
						message.append("command = " + script.get("command"), true);
						message.append("component = " + script.get("component"), true);
						message.append("message = " + script.get("message"), true);
						message.append("response = " + script.get("response"), true);
						List<LinkedHashMap<String, Object>> errors = (List<LinkedHashMap<String, Object>>) script.get("errors");
						if (errors != null) {
							message.appendSeparator();
							for (LinkedHashMap<String, Object> error : errors) {
								message.append(error.get("message").toString(), true);
							}
						}
					}
				}
			}

		} catch (Exception e) {
			numErrors = 1;
			message.append("exception = " + e.getMessage());
		}


		if (numErrors > 0) {
			message.showError();
		} else if (numWarnings > 0) {
			message.showWarning();
		} else {
			message.showInformation();
		}

	}

	public void drop(VirtualFile selectedFile) {
		ToolboxProject toolboxProject = null;

		Message message = toolboxProject.newMessage();
		message.setTitle("Drop from Vault: " + selectedFile.getName());

		int numErrors = 0;
		int numWarnings = 0;
		try {

			//get either the MDL or SDK response
			VaultResponse response = null;
			if (selectedFile.getName().toLowerCase().endsWith(".mdl")) {
				String dropMdl = "DROP " + selectedFile.getName().replace(".mdl", "").replace(".", " ") + ";";
				response = toolboxProject.getVaultClient().newRequest(MetaDataRequest.class)
						.setRequestString(dropMdl)
						.executeMDLScript();
			} else if (selectedFile.getName().toLowerCase().endsWith(".java")) {
				PsiFile psiFile = PsiManager.getInstance(toolboxProject.getProject()).findFile(selectedFile);
				if (psiFile instanceof PsiJavaFile psiJavaFile) {
					String className = psiJavaFile.getPackageName() + "." + psiJavaFile.getName().replace(".java", "");
					response = toolboxProject.getVaultClient().newRequest(SDKRequest.class)
							.setBinaryFile(psiFile.getName(), psiFile.getText().getBytes(StandardCharsets.UTF_8))
							.deleteSingleSourceCodeFile(className);
				}
			}

			//if we have a response, show it
			if (response == null) {
				message.append("response = null");
			}
			else {
				toolboxProject.removeFile(selectedFile.getCanonicalPath());

				message.append(response.getResponseStatus(), true);
				if (response.getResponseMessage() != null) {
					message.appendSeparator();
					message.append(response.getResponseMessage(), true);
				}

				if (response.hasErrors()) {
					message.appendSeparator();
					for (VaultResponse.APIResponseError error : response.getErrors()) {
						message.append(error.getMessage(), true);
						numErrors++;
					}
				}

				LinkedHashMap<String, Object> scriptExecution = (LinkedHashMap<String, Object>) response.get("script_execution");
				if (scriptExecution != null) {
					numWarnings = (int) scriptExecution.get("warnings");
					numErrors = numErrors + (int) scriptExecution.get("failures") + (int) scriptExecution.get("exceptions");

					message.appendSeparator();
					message.append("message = " + scriptExecution.get("message"), true);
					message.append("warnings = " + scriptExecution.get("warnings"), true);
					message.append("failures = " + scriptExecution.get("failures"), true);
					message.append("exceptions = " + scriptExecution.get("exceptions"), true);
					message.append("components_affected = " + scriptExecution.get("components_affected"), true);
				}

				List<LinkedHashMap<String, Object>> statementExecutions = (List<LinkedHashMap<String, Object>>) response.get("statement_execution");
				if (statementExecutions != null) {
					for (LinkedHashMap<String, Object> script : statementExecutions) {
						message.appendSeparator();
						message.append("command = " + script.get("command"), true);
						message.append("component = " + script.get("component"), true);
						message.append("message = " + script.get("message"), true);
						message.append("response = " + script.get("response"), true);
						List<LinkedHashMap<String, Object>> errors = (List<LinkedHashMap<String, Object>>) script.get("errors");
						if (errors != null) {
							message.appendSeparator();
							for (LinkedHashMap<String, Object> error : errors) {
								message.append(error.get("message").toString(), true);
							}
						}
					}
				}
			}

		}
		catch (Exception e) {
			numErrors = 1;
			message.append("exception = " + e.getMessage());
		}

		if (numErrors > 0) {
			message.showError();
		}
		else if (numWarnings > 0) {
			message.showWarning();
		}
		else {
			message.showInformation();
		}
	}
}
