package com.coy.l2cache.test;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
//@EqualsAndHashCode
public class UserDTO {

    public UserDTO() {
    }

    public UserDTO(String name, String userId) {
        this.name = name;
        this.userId = userId;
    }

    private String userId;
    private String name;

    public boolean equals(Object obj) {
        return (this == obj);
    }

    public String toString() {
        return getClass().getName() + "@" + Integer.toHexString(hashCode());
    }

}
