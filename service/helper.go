package service

import (
	"crypto/md5"
	"encoding/hex"
	"encoding/json"
	"fmt"
	"io/ioutil"
	"log"
	"os"
	"regexp"

	"github.com/jasonlvhit/gocron"
	"github.com/jaypipes/ghw"
)

type hardware struct {
	BaseboardName string `json:"b"`
	NumOfCore     uint32 `json:"c"`
	TotalOfDisk   uint64 `json:"d"`
	TotalMemory   int64  `json:"r"`
	MacAddr       string `json:"m"`
}

//GetMacAddr ...
func GetMacAddr() (string, error) {

	network, err := ghw.Network()
	if err != nil {
		log.Fatal(err)
	}
	var MacAddr string
	for _, nic := range network.NICs {
		MacAddr = nic.MacAddress
	}
	return MacAddr, nil
}

//Getcpuinfo ...
func Getcpuinfo() (uint32, error) {

	cpu, err := ghw.CPU()
	if err != nil {
		log.Fatal(err)
	}
	NumOfCore := cpu.TotalThreads
	return NumOfCore, nil
}

//Getmemory ...
func Getmemory() (int64, error) {

	memory, err := ghw.Memory()
	if err != nil {
		log.Fatal(err)
	}
	TotalMemory := memory.TotalPhysicalBytes / 1024000000
	return TotalMemory, nil
}

//Getbaseboard ...
func Getbaseboard() (string, error) {

	baseboard, err := ghw.Baseboard()
	if err != nil {
		log.Fatal(err)
	}
	BaseboardName := baseboard.Vendor
	return BaseboardName, nil
}

//Getinfoblock is get usage disk
func Getinfoblock() (uint64, error) {
	disk, err := ghw.Block()
	if err != nil {
		log.Fatal(err)
	}
	TotalOfDisk := disk.TotalPhysicalBytes / 1024000000
	return TotalOfDisk, nil
}

//Gethardware info
func Gethardware() string {
	info := new(hardware)
	info.BaseboardName, _ = Getbaseboard()
	info.NumOfCore, _ = Getcpuinfo()
	info.TotalMemory, _ = Getmemory()
	info.TotalOfDisk, _ = Getinfoblock()
	info.MacAddr, _ = GetMacAddr()
	jsonHw, err := json.Marshal(info)
	if err != nil {
		log.Fatal(err)
	}
	temp := string(jsonHw)
	reg, err := regexp.Compile("[^a-zA-Z0-9]+")
	if err != nil {
		log.Fatal(err)
	}
	strHW := reg.ReplaceAllString(temp, "")
	return strHW
}

//HashInfo to md5
func HashInfo() (string, error) {
	hwInfo := Gethardware()
	hasher := md5.New()
	hasher.Write([]byte(hwInfo))
	md5Data := hex.EncodeToString(hasher.Sum(nil))
	return md5Data, nil
}

//SaveData to file
func SaveData() (string, error) {
	md5Data, _ := HashInfo()
	err := ioutil.WriteFile("data.txt", []byte(md5Data), 0644)
	if err != nil {
		return "", err
	}
	return "", nil
}

//CheckHw info
func CheckHw() (string, error) {
	hw, _ := HashInfo()
	data, err := ioutil.ReadFile("data.txt")
	if err != nil {
		return "", err
	}
	if hw != string(data) {
		fmt.Println("Invalid Hardware Infomation")
		os.Exit(1)
	}

	return "", nil
}

//ExecCronjob schedule job check
func ExecCronjob() {
	gocron.Every(1).Days().Do(CheckHw)
	<-gocron.Start()

}
