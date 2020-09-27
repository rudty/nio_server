package main

import (
	"fmt"
	"log"
	"net"
)

func main() {
	d, _ := net.Dial("tcp", ":8080")
	// d.Write([]byte("hello world"))

	var buf [256]byte
	for {
		go func() {
			_, err := d.Write([]byte("hello world"))
			if err != nil {
				log.Fatal(err)
			}
			fmt.Println("send")
		}()
		_, err := d.Read(buf[:])
		if err != nil {
			log.Fatal(err)
		}
		fmt.Println("server>>", string(buf[:]))
		// time.Sleep(1 * time.Second)
	}
}
