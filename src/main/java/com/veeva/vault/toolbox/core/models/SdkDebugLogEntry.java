package com.veeva.vault.toolbox.core.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.veeva.vault.vapil.api.model.VaultModel;

/**
 * Represents a single entry in a Vault SDK debug log, containing execution
 * metrics such as elapsed time, CPU time, and memory usage.
 */
public class SdkDebugLogEntry extends VaultModel {

	/**
	 * Returns the timestamp of this log entry.
	 *
	 * @return the timestamp string
	 */
	@JsonProperty("timestamp")
	public String getTimestamp() {
		return this.getString("timestamp");
	}

	/**
	 * Sets the timestamp of this log entry.
	 *
	 * @param timestamp the timestamp string
	 */
	public void setTimestamp(String timestamp) {
		this.set("timestamp", timestamp);
	}

	/**
	 * Returns the execution ID associated with this log entry.
	 *
	 * @return the execution ID
	 */
	@JsonProperty("execution_id")
	public String getExecutionId() {
		return this.getString("execution_id");
	}

	/**
	 * Sets the execution ID associated with this log entry.
	 *
	 * @param executionId the execution ID
	 */
	public void setExecutionId(String executionId) {
		this.set("execution_id", executionId);
	}

	/**
	 * Returns the Vault ID associated with this log entry.
	 *
	 * @return the Vault ID
	 */
	@JsonProperty("vault_id")
	public String getVaultId() {
		return this.getString("vault_id");
	}

	/**
	 * Sets the Vault ID associated with this log entry.
	 *
	 * @param vaultId the Vault ID
	 */
	public void setVaultId(String vaultId) {
		this.set("vault_id", vaultId);
	}

	/**
	 * Returns the user ID associated with this log entry.
	 *
	 * @return the user ID
	 */
	@JsonProperty("user_id")
	public String getUserId() {
		return this.getString("user_id");
	}

	/**
	 * Sets the user ID associated with this log entry.
	 *
	 * @param userId the user ID
	 */
	public void setUserId(String userId) {
		this.set("user_id", userId);
	}

	/**
	 * Returns the transaction ID associated with this log entry.
	 *
	 * @return the transaction ID
	 */
	@JsonProperty("transaction_id")
	public String getTransactionId() {
		return this.getString("transaction_id");
	}

	/**
	 * Sets the transaction ID associated with this log entry.
	 *
	 * @param transactionId the transaction ID
	 */
	public void setTransactionId(String transactionId) {
		this.set("transaction_id", transactionId);
	}

	/**
	 * Returns the log file name associated with this log entry.
	 *
	 * @return the log file name
	 */
	@JsonProperty("log_file")
	public String getLogFile() {
		return this.getString("log_file");
	}

	/**
	 * Sets the log file name associated with this log entry.
	 *
	 * @param logFile the log file name
	 */
	public void setLogFile(String logFile) {
		this.set("log_file", logFile);
	}

	/**
	 * Returns the type of this log entry.
	 *
	 * @return the log entry type
	 */
	@JsonProperty("type")
	public String getType() {
		return this.getString("type");
	}

	/**
	 * Sets the type of this log entry.
	 *
	 * @param type the log entry type
	 */
	public void setType(String type) {
		this.set("type", type);
	}

	/**
	 * Returns the category of this log entry.
	 *
	 * @return the log entry category
	 */
	@JsonProperty("category")
	public String getCategory() {
		return this.getString("category");
	}

	/**
	 * Sets the category of this log entry.
	 *
	 * @param category the log entry category
	 */
	public void setCategory(String category) {
		this.set("category", category);
	}

	/**
	 * Returns the class name associated with this log entry.
	 *
	 * @return the class name
	 */
	@JsonProperty("class_name")
	public String getClassName() {
		return this.getString("class_name");
	}

	/**
	 * Sets the class name associated with this log entry.
	 *
	 * @param className the class name
	 */
	public void setClassName(String className) {
		this.set("class_name", className);
	}

	/**
	 * Returns the combined service and method identifier in {@code ServiceName#MethodName} format.
	 *
	 * @return the service method string
	 */
	@JsonProperty("service_method")
	public String getServiceMethod() {
		return this.getString("service_method");
	}

	/**
	 * Sets the combined service and method identifier.
	 *
	 * @param serviceMethod the service method string in {@code ServiceName#MethodName} format
	 */
	public void setServiceMethod(String serviceMethod) {
		this.set("service_method", serviceMethod);
	}

	/**
	 * Returns the service name derived from the {@code service_method} field.
	 *
	 * @return the service name, or {@code null} if not set
	 */
	@JsonProperty("service_name")
	public String getServiceName() {
		String serviceMethod = getServiceMethod();
		if (serviceMethod != null) {
			return serviceMethod.substring(0, serviceMethod.indexOf("#"));
		}
		return null;
	}

	/**
	 * Returns the method name derived from the {@code service_method} field.
	 *
	 * @return the method name, or {@code null} if not set
	 */
	@JsonProperty("method_name")
	public String getMethodName() {
		String serviceMethod = getServiceMethod();
		if (serviceMethod != null) {
			return serviceMethod.substring(serviceMethod.indexOf("#") + 1);
		}
		return null;
	}

	/**
	 * Returns the elapsed time in milliseconds.
	 *
	 * @return the elapsed time in milliseconds
	 */
	@JsonProperty("elapsed_time_ms")
	public Long getElapsedTime() {
		return getLong("elapsed_time_ms", 0L);
	}

	/**
	 * Sets the elapsed time in milliseconds.
	 *
	 * @param elapsedTime the elapsed time in milliseconds
	 */
	public void setElapsedTime(Long elapsedTime) {
		this.set("elapsed_time_ms", elapsedTime);
	}

	/**
	 * Returns the elapsed time in seconds.
	 *
	 * @return the elapsed time in seconds
	 */
	@JsonProperty("elapsed_time_seconds")
	public Double getElapsedTimeSeconds() {
		return getLongAsDouble(getElapsedTime(), 1000);
	}

	/**
	 * Returns the CPU time in nanoseconds.
	 *
	 * @return the CPU time in nanoseconds
	 */
	@JsonProperty("cpu_time_ns")
	public Long getCpuTime() {
		return getLong("cpu_time_ns", 0L);
	}

	/**
	 * Sets the CPU time in nanoseconds.
	 *
	 * @param cpuTime the CPU time in nanoseconds
	 */
	public void setCpuTime(Long cpuTime) {
		this.set("cpu_time_ns", cpuTime);
	}

	/**
	 * Returns the CPU time in seconds.
	 *
	 * @return the CPU time in seconds
	 */
	@JsonProperty("cpu_time_seconds")
	public Double getCpuTimeSeconds() {
		return getLongAsDouble(getCpuTime(), 1000000000);
	}

	/**
	 * Returns the memory usage in bytes.
	 *
	 * @return the memory usage in bytes
	 */
	@JsonProperty("memory")
	public Long getMemory() {
		return getLong("memory", 0L);
	}

	/**
	 * Sets the memory usage in bytes.
	 *
	 * @param memory the memory usage in bytes
	 */
	public void setMemory(Long memory) {
		this.set("memory", memory);
	}

	/**
	 * Returns the memory usage in megabytes.
	 *
	 * @return the memory usage in megabytes
	 */
	@JsonProperty("memory_mb")
	public Double getMemoryMb() {
		return getLongAsDouble(getMemory(), 1000000);
	}

	/**
	 * Returns the gross memory usage in bytes.
	 *
	 * @return the gross memory usage in bytes
	 */
	@JsonProperty("gross_memory")
	public Long getGrossMemory() {
		return getLong("gross_memory", 0L);
	}

	/**
	 * Sets the gross memory usage in bytes.
	 *
	 * @param grossMemory the gross memory usage in bytes
	 */
	public void setGrossMemory(Long grossMemory) {
		this.set("gross_memory", grossMemory);
	}

	/**
	 * Returns the gross memory usage in megabytes.
	 *
	 * @return the gross memory usage in megabytes
	 */
	@JsonProperty("gross_memory_mb")
	public Double getGrossMemoryMb() {
		return getLongAsDouble(getGrossMemory(), 1000000);
	}

	/**
	 * Returns the number of times this entry was invoked.
	 *
	 * @return the invocation count
	 */
	@JsonProperty("invocation_count")
	public Long getInvocationCount() {
		return getLong("invocation_count", 0L);
	}

	/**
	 * Sets the number of times this entry was invoked.
	 *
	 * @param invocationCount the invocation count
	 */
	public void setInvocationCount(Long invocationCount) {
		this.set("invocation_count", invocationCount);
	}

	/**
	 * Returns the log message for this entry.
	 *
	 * @return the log message
	 */
	@JsonProperty("message")
	public String getMessage() {
		return this.getString("message");
	}

	/**
	 * Sets the log message for this entry.
	 *
	 * @param message the log message
	 */
	public void setMessage(String message) {
		this.set("message", message);
	}

	/**
	 * Retrieves a Long value for the specified field, returning a default value if the field is null.
	 *
	 * @param fieldName    the name of the field to retrieve
	 * @param defaultValue the value to return if the field is null
	 * @return the Long value or the default
	 */
	private Long getLong(String fieldName, Long defaultValue) {
		Long value = this.getLong(fieldName);
		return value != null ? value : defaultValue;
	}

	/**
	 * Converts a Long value to a Double by dividing it by a specified factor.
	 *
	 * @param value  the Long value to convert
	 * @param factor the division factor (e.g., 1000 for milliseconds to seconds)
	 * @return the resulting Double value, or {@code null} if the input value was null
	 */
	private Double getLongAsDouble(Long value, Integer factor) {
		if (value == null) {
			return null;
		}
		return (factor != null && factor > 0) ? (double) value / factor : (double) value;
	}
}
