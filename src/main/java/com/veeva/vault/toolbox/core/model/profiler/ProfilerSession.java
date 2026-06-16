package com.veeva.vault.toolbox.core.model.profiler;

/**
 * Represents a Vault SDK profiler session metadata record.
 */
public class ProfilerSession {

    /** The unique identifier of the profiler session. */
    private String id;
    /** The API name of the profiler session. */
    private String name;
    /** The display label of the profiler session. */
    private String label;
    /** The current status of the profiler session (e.g., active, inactive). */
    private String status;
    /** A detailed description of the profiler session. */
    private String description;
    /** The ID of the user associated with the profiler session. */
    private String user;
    /** The display name of the user associated with the profiler session. */
    private String userName;
    /** The date and time when the profiler session was created. */
    private String createdDate;
    /** The date and time when the profiler session is set to expire. */
    private String expirationDate;

    /**
     * Returns the unique identifier of this profiler session.
     *
     * @return the session ID
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the unique identifier of this profiler session.
     *
     * @param id the session ID
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Returns the API name of this profiler session.
     *
     * @return the session name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the API name of this profiler session.
     *
     * @param name the session name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the display label of this profiler session.
     *
     * @return the session label
     */
    public String getLabel() {
        return label;
    }

    /**
     * Sets the display label of this profiler session.
     *
     * @param label the session label
     */
    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * Returns the current status of this profiler session.
     *
     * @return the session status
     */
    public String getStatus() {
        return status;
    }

    /**
     * Sets the current status of this profiler session.
     *
     * @param status the session status
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * Returns the description of this profiler session.
     *
     * @return the session description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description of this profiler session.
     *
     * @param description the session description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Returns the user ID associated with this profiler session.
     *
     * @return the user ID
     */
    public String getUser() {
        return user;
    }

    /**
     * Sets the user ID associated with this profiler session.
     *
     * @param user the user ID
     */
    public void setUser(String user) {
        this.user = user;
    }

    /**
     * Returns the display name of the user associated with this profiler session.
     *
     * @return the user display name
     */
    public String getUserName() {
        return userName;
    }

    /**
     * Sets the display name of the user associated with this profiler session.
     *
     * @param userName the user display name
     */
    public void setUserName(String userName) {
        this.userName = userName;
    }

    /**
     * Returns the creation date of this profiler session.
     *
     * @return the created date string
     */
    public String getCreatedDate() {
        return createdDate;
    }

    /**
     * Sets the creation date of this profiler session.
     *
     * @param createdDate the created date string
     */
    public void setCreatedDate(String createdDate) {
        this.createdDate = createdDate;
    }

    /**
     * Returns the expiration date of this profiler session.
     *
     * @return the expiration date string
     */
    public String getExpirationDate() {
        return expirationDate;
    }

    /**
     * Sets the expiration date of this profiler session.
     *
     * @param expirationDate the expiration date string
     */
    public void setExpirationDate(String expirationDate) {
        this.expirationDate = expirationDate;
    }
}
