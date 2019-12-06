/**
 * FileName: Address
 * Author:   yangqinkuan
 * Date:     2019-12-5 15:08
 * Description:
 */

package com.ggrpc.common.rpc;

public class Address {
    // 地址
    private String host;
    // 端口
    private int port;

    public Address() {
    }

    public Address(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        Address address = (Address) o;

        return port == address.port && !(host != null ? !host.equals(address.host) : address.host != null);
    }

    @Override
    public int hashCode() {
        int result = host != null ? host.hashCode() : 0;
        result = 31 * result + port;
        return result;
    }

    @Override
    public String toString() {
        return "Address{" + "host='" + host + '\'' + ", port=" + port + '}';
    }
}
