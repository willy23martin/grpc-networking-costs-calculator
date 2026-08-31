project-name = "glialli23"
aws-region = "us-east-1"

CIDR-Block-VPC="10.10.0.0/16"
ssh-port=22
dns-resolution = false

cidr-private-subnets = [
    "10.10.0.0/20", // which provides up to 4,094 usable hosts per subnet
    "10.10.16.0/20" // which provides up to 4,094 usable hosts per subnet
]

private-subnet = {
    "vpc" = "vpc-387381552",
    "cidr" = "10.10.0.0/20",
    "zone" = "us-east-1",
    "name" = "calculator-subnet",
    "map_ip" = "True"
}