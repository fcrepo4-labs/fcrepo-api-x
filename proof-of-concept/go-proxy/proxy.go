package main

import (
	"log"
	"net/http"
	"net/http/httputil"
	"net/url"
	"os"
)

// Right now, this is a really simple proxy that uses defaults
// nearly everywhere, and does nothing other than blindly proxy a
// request to a single server.
func main() {

	bind, provided := os.LookupEnv("BIND_ADDR")
	if !provided {
		bind = ":8090"
	}
	log.Println("Binding to", bind)

	proxyHost, provided := os.LookupEnv("PROXY_ADDR")
	if !provided {
		proxyHost = "127.0.0.1:8080"
	}
	log.Println("Proxying host", proxyHost)

	proxyPath, provided := os.LookupEnv("PROXY_PATH")
	if !provided {
		proxyPath = "/"
	}
	log.Println("Proxy Path", proxyPath)

	proxy := httputil.NewSingleHostReverseProxy(&url.URL{Host: proxyHost, Path: proxyPath, Scheme: "http"})

	if err := http.ListenAndServe(bind, proxy); err != nil {
		log.Panic("Could not start proxy", err)
	}
}
